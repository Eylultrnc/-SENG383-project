
# beeplan/scheduler.py
from __future__ import annotations
from typing import Dict, Iterable, List, Optional, Tuple
import logging

from .exceptions import DataValidationError, SchedulingError
from .models import (
    Assignment, Course, DayOfWeek, Instructor, Room, RoomType, Schedule,
    Session, SessionType, TimeSlot
)
from .constraints import (
    Constraint, ConstraintResult, ScheduleContext,
    NoExamBlockConstraint, InstructorAvailabilityConstraint,
    MaxTheoryHoursPerInstructorPerDayConstraint, RoomCapacityConstraint,
    RoomTypeCompatibilityConstraint, NoRoomOverlapConstraint,
    NoInstructorOverlapConstraint, LabAfterTheoryConstraint,
    ThirdYearVsElectivesNoOverlapConstraint, CENGvsSENGElectivesNoOverlapConstraint,
    PreferConsecutiveLabHoursConstraint
)

logger = logging.getLogger("BeePlanScheduler")
logger.setLevel(logging.INFO)

def _validate_inputs(courses: List[Course], instructors: List[Instructor], rooms: List[Room], timeslots: List[TimeSlot]):
    if not courses:
        raise DataValidationError("No courses provided.")
    if not instructors:
        raise DataValidationError("No instructors provided.")
    if not rooms:
        raise DataValidationError("No rooms provided.")
    if not timeslots:
        raise DataValidationError("No timeslots provided.")

    instr_ids = {i.id for i in instructors}
    for c in courses:
        if c.instructor_id not in instr_ids:
            raise DataValidationError(f"Course {c.id} references unknown instructor {c.instructor_id}.")
        if c.lab_hours_per_week > 0 and c.enrolled_students > 40:
            # We warn here; the hard constraint will block placement unless the user splits groups.
            logger.warning(f"[Warn] Course {c.id} lab has {c.enrolled_students} students > 40; split groups recommended.")

class Scheduler:
    """
    Public API:
        Scheduler(constraints: Optional[List[Constraint]] = None)
        generate_schedule(courses, instructors, rooms, timeslots) -> Schedule
        get_report() -> Dict[str, List[str]]
    """
    def __init__(self, constraints: Optional[List[Constraint]] = None):
        self.hard_constraints: List[Constraint] = []
        self.soft_constraints: List[Constraint] = []
        self.report: Dict[str, List[str]] = {"warnings": [], "violations": []}

        default_constraints = constraints or [
            NoExamBlockConstraint(),
            InstructorAvailabilityConstraint(),
            MaxTheoryHoursPerInstructorPerDayConstraint(),
            RoomCapacityConstraint(),
            RoomTypeCompatibilityConstraint(),
            NoRoomOverlapConstraint(),
            NoInstructorOverlapConstraint(),
            LabAfterTheoryConstraint(),
            ThirdYearVsElectivesNoOverlapConstraint(),
            CENGvsSENGElectivesNoOverlapConstraint(),
            PreferConsecutiveLabHoursConstraint(),  # soft
        ]
        for c in default_constraints:
            (self.soft_constraints if not c.hard else self.hard_constraints).append(c)

    # --- Public methods ---
    def generate_schedule(
        self,
        courses: List[Course],
        instructors: List[Instructor],
        rooms: List[Room],
        timeslots: List[TimeSlot]
    ) -> Schedule:
        """Main entry point. Raises SchedulingError if unsatisfiable."""
        _validate_inputs(courses, instructors, rooms, timeslots)

        schedule = Schedule()
        course_by_id = {c.id: c for c in courses}
        instr_by_id = {i.id: i for i in instructors}
        room_by_id = {r.id: r for r in rooms}
        course_to_instructor = {c.id: c.instructor_id for c in courses}

        ctx = ScheduleContext(course_by_id, instr_by_id, room_by_id, timeslots, course_to_instructor)
        sessions = self._build_sessions(courses)
        ordered = self._order_sessions(sessions, ctx)

        if not self._backtrack(ordered, schedule, ctx):
            raise SchedulingError("No conflict-free schedule could be generated with current inputs.")
        return schedule

    def get_report(self) -> Dict[str, List[str]]:
        return self.report

    # --- Internal helpers ---
    def _build_sessions(self, courses: List[Course]) -> List[Session]:
        sessions: List[Session] = []
        for c in courses:
            for idx in range(c.theory_hours_per_week):
                sessions.append(Session(course_id=c.id, session_type=SessionType.THEORY, duration_hours=1, index=idx))
            for idx in range(c.lab_hours_per_week):
                sessions.append(Session(course_id=c.id, session_type=SessionType.LAB, duration_hours=1, index=idx))
        return sessions

    def _order_sessions(self, sessions: List[Session], ctx: ScheduleContext) -> List[Session]:
        """Heuristic: theory before labs (to satisfy lab-after-theory), prioritize 3rd/electives."""
        def score(s: Session) -> Tuple[int, int, int]:
            c = ctx.courses[s.course_id]
            primary = 0 if s.session_type == SessionType.THEORY else 1
            secondary = 0 if (c.year == 3 or c.is_elective or c.department in {"CENG", "SENG"}) else 1
            tertiary = -(c.theory_hours_per_week + c.lab_hours_per_week)
            return (primary, secondary, tertiary)
        return sorted(sessions, key=score)

    def _candidate_assignments(self, session: Session, ctx: ScheduleContext) -> Iterable[Assignment]:
        course = ctx.courses[session.course_id]
        # Room filters
        rooms = list(ctx.rooms.values())
        if session.session_type == SessionType.LAB:
            rooms = [r for r in rooms if r.type == RoomType.LAB]
        # Capacity prefilter
        if session.session_type == SessionType.THEORY:
            rooms = [r for r in rooms if r.capacity >= course.enrolled_students]
        else:
            rooms = [r for r in rooms if r.capacity >= min(course.enrolled_students, 40)]

        for ts in ctx.timeslots:
            for r in rooms:
                yield Assignment(session=session, timeslot=ts, room_id=r.id)

    def _check_hard(self, schedule: Schedule, assignment: Assignment, ctx: ScheduleContext) -> Tuple[bool, List[str]]:
        reasons = []
        for c in self.hard_constraints:
            res = c.check(schedule, assignment, ctx)
            if not res.ok:
                reasons.append(res.message or c.__class__.__name__)
        return (len(reasons) == 0, reasons)

    def _soft_penalty(self, schedule: Schedule, assignment: Assignment, ctx: ScheduleContext) -> int:
        penalty = 0
        for c in self.soft_constraints:
            res = c.check(schedule, assignment, ctx)
            if not res.ok:
                penalty += c.weight
        return penalty

    def _backtrack(self, sessions: List[Session], schedule: Schedule, ctx: ScheduleContext, depth: int = 0) -> bool:
        if not sessions:
            return True

        session = sessions[0]
        candidates: List[Tuple[int, Assignment]] = []

        for cand in self._candidate_assignments(session, ctx):
            ok, reasons = self._check_hard(schedule, cand, ctx)
            if ok:
                penalty = self._soft_penalty(schedule, cand, ctx)
                candidates.append((penalty, cand))
            else:
                # Store violations for diagnostics (not fatal here)
                for r in reasons:
                    self.report["violations"].append(f"[{session.course_id} {session.session_type.name}] {r}")

        # Prefer smaller penalty (better soft-constraint satisfaction)
        candidates.sort(key=lambda x: x[0])

        for _, cand in candidates:
            schedule.add(cand, ctx.course_to_instructor)
            if self._backtrack(sessions[1:], schedule, ctx, depth + 1):
                return True
            schedule.remove(cand, ctx.course_to_instructor)

        return False
