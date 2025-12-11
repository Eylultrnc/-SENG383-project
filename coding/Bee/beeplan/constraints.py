
# beeplan/constraints.py
from __future__ import annotations
from dataclasses import dataclass
from typing import Dict, List, Optional, Tuple

from .models import (
    DayOfWeek, TimeSlot, RoomType, SessionType, Schedule, Assignment, Course, Instructor, Room
)

# --- Constraint framework ---
@dataclass
class ConstraintResult:
    ok: bool
    message: Optional[str] = None

class Constraint:
    """Base class for constraints."""
    hard: bool = True  # soft constraints set this to False
    weight: int = 0    # penalty for soft constraints

    def check(self, schedule: Schedule, assignment: Assignment, ctx: "ScheduleContext") -> ConstraintResult:
        raise NotImplementedError

@dataclass
class ScheduleContext:
    """Shared context: lookup tables + global parameters."""
    courses: Dict[str, Course]
    instructors: Dict[str, Instructor]
    rooms: Dict[str, Room]
    timeslots: List[TimeSlot]
    course_to_instructor: Dict[str, str]
    exam_block: Tuple[DayOfWeek, str, str] = (DayOfWeek.FRI, "13:20", "15:10")

# --- Hard constraints ---
class NoExamBlockConstraint(Constraint):
    def check(self, schedule: Schedule, assignment: Assignment, ctx: ScheduleContext) -> ConstraintResult:
        day, start, end = ctx.exam_block
        ts = assignment.timeslot
        if ts.day != day:
            return ConstraintResult(True)
        # overlap against the exam-block interval
        block = TimeSlot(day, start, end)
        if ts.overlaps(block):
            return ConstraintResult(False, "No courses during Friday 13:20–15:10 exam block.")
        return ConstraintResult(True)

class MaxTheoryHoursPerInstructorPerDayConstraint(Constraint):
    MAX_HOURS = 4

    def check(self, schedule: Schedule, assignment: Assignment, ctx: ScheduleContext) -> ConstraintResult:
        if assignment.session.session_type != SessionType.THEORY:
            return ConstraintResult(True)
        course_id = assignment.session.course_id
        instr_id = ctx.course_to_instructor[course_id]
        key = (instr_id, assignment.timeslot.day)
        hours = 0
        for a in schedule.by_instructor_day.get(key, []):
            if a.session.session_type == SessionType.THEORY:
                hours += a.session.duration_hours
        if hours + assignment.session.duration_hours > self.MAX_HOURS:
            return ConstraintResult(False, "Instructor exceeds max 4 theory hours per day.")
        return ConstraintResult(True)

class InstructorAvailabilityConstraint(Constraint):
    def check(self, schedule: Schedule, assignment: Assignment, ctx: ScheduleContext) -> ConstraintResult:
        instr_id = ctx.course_to_instructor[assignment.session.course_id]
        instr = ctx.instructors[instr_id]
        if not instr.is_available(assignment.timeslot):
            return ConstraintResult(False, "Instructor not available in this timeslot.")
        return ConstraintResult(True)

class RoomCapacityConstraint(Constraint):
    def check(self, schedule: Schedule, assignment: Assignment, ctx: ScheduleContext) -> ConstraintResult:
        course = ctx.courses[assignment.session.course_id]
        room = ctx.rooms[assignment.room_id]
        if assignment.session.session_type == SessionType.LAB:
            if course.enrolled_students > 40:
                return ConstraintResult(False, "Lab capacity exceeds 40; split into groups required.")
            if room.capacity < min(course.enrolled_students, 40):
                return ConstraintResult(False, "Selected lab room capacity insufficient.")
        else:
            if room.capacity < course.enrolled_students:
                return ConstraintResult(False, "Selected classroom capacity insufficient.")
        return ConstraintResult(True)

class RoomTypeCompatibilityConstraint(Constraint):
    def check(self, schedule: Schedule, assignment: Assignment, ctx: ScheduleContext) -> ConstraintResult:
        room = ctx.rooms[assignment.room_id]
        if assignment.session.session_type == SessionType.LAB and room.type != RoomType.LAB:
            return ConstraintResult(False, "Lab sessions must be in lab rooms.")
        return ConstraintResult(True)

class NoRoomOverlapConstraint(Constraint):
    def check(self, schedule: Schedule, assignment: Assignment, ctx: ScheduleContext) -> ConstraintResult:
        key = (assignment.room_id, assignment.timeslot.day)
        for a in schedule.by_room_day.get(key, []):
            if a.timeslot.overlaps(assignment.timeslot):
                return ConstraintResult(False, "Room overlap conflict.")
        return ConstraintResult(True)

class NoInstructorOverlapConstraint(Constraint):
    def check(self, schedule: Schedule, assignment: Assignment, ctx: ScheduleContext) -> ConstraintResult:
        instr_id = ctx.course_to_instructor[assignment.session.course_id]
        key = (instr_id, assignment.timeslot.day)
        for a in schedule.by_instructor_day.get(key, []):
            if a.timeslot.overlaps(assignment.timeslot):
                return ConstraintResult(False, "Instructor overlap conflict.")
        return ConstraintResult(True)

class LabAfterTheoryConstraint(Constraint):
    def check(self, schedule: Schedule, assignment: Assignment, ctx: ScheduleContext) -> ConstraintResult:
        if assignment.session.session_type != SessionType.LAB:
            return ConstraintResult(True)
        course_id = assignment.session.course_id
        theory_before = [
            a for a in schedule.by_course.get(course_id, [])
            if a.session.session_type == SessionType.THEORY and a.timeslot.starts_before(assignment.timeslot)
        ]
        if not theory_before:
            return ConstraintResult(False, "Lab must be scheduled after theory.")
        return ConstraintResult(True)

class ThirdYearVsElectivesNoOverlapConstraint(Constraint):
    def check(self, schedule: Schedule, assignment: Assignment, ctx: ScheduleContext) -> ConstraintResult:
        course = ctx.courses[assignment.session.course_id]
        if not (course.year == 3 or course.is_elective):
            return ConstraintResult(True)

        for a in schedule.assignments:
            other = ctx.courses[a.session.course_id]
            mixed = ((course.year == 3 and other.is_elective) or (course.is_elective and other.year == 3))
            if mixed and a.timeslot.day == assignment.timeslot.day and a.timeslot.overlaps(assignment.timeslot):
                return ConstraintResult(False, "3rd-year courses must not overlap with electives.")
        return ConstraintResult(True)

class CENGvsSENGElectivesNoOverlapConstraint(Constraint):
    def check(self, schedule: Schedule, assignment: Assignment, ctx: ScheduleContext) -> ConstraintResult:
        course = ctx.courses[assignment.session.course_id]
        if not (course.is_elective and course.department in {"CENG", "SENG"}):
            return ConstraintResult(True)

        for a in schedule.assignments:
            other = ctx.courses[a.session.course_id]
            if other.is_elective and {course.department, other.department} == {"CENG", "SENG"}:
                if a.timeslot.day == assignment.timeslot.day and a.timeslot.overlaps(assignment.timeslot):
                    return ConstraintResult(False, "CENG and SENG electives must not overlap.")
        return ConstraintResult(True)

# --- Soft constraint ---
class PreferConsecutiveLabHoursConstraint(Constraint):
    hard = False
    weight = 10

    def check(self, schedule: Schedule, assignment: Assignment, ctx: ScheduleContext) -> ConstraintResult:
        if assignment.session.session_type != SessionType.LAB:
            return ConstraintResult(True)

        course_id = assignment.session.course_id
        labs = [a for a in schedule.by_course.get(course_id, []) if a.session.session_type == SessionType.LAB]
        for a in labs:
            if a.timeslot.day == assignment.timeslot.day:
                # Prefer adjacent hours (end == start or start == end)
                if not (assignment.timeslot.start == a.timeslot.end or a.timeslot.start == assignment.timeslot.end):
                    return ConstraintResult(False, "Prefer consecutive lab hours.")
