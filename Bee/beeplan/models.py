
# beeplan/models.py
from __future__ import annotations
from dataclasses import dataclass, field
from enum import Enum, auto
from typing import Dict, List, Optional, Tuple

# --- Small time utilities (minutes since midnight) ---
def _to_minutes(hhmm: str) -> int:
    h, m = map(int, hhmm.split(":"))
    return h * 60 + m

class DayOfWeek(Enum):
    MON = 1
    TUE = 2
    WED = 3
    THU = 4
    FRI = 5

@dataclass(frozen=True)
class TimeSlot:
    """A concrete interval on a specific day (closed-open [start, end))."""
    day: DayOfWeek
    start: str  # "HH:MM"
    end: str    # "HH:MM"

    @property
    def start_min(self) -> int:
        return _to_minutes(self.start)

    @property
    def end_min(self) -> int:
        return _to_minutes(self.end)

    def overlaps(self, other: "TimeSlot") -> bool:
        if self.day != other.day:
            return False
        return not (self.end_min <= other.start_min or other.end_min <= self.start_min)

    def starts_before(self, other: "TimeSlot") -> bool:
        return (self.day.value < other.day.value) or \
               (self.day == other.day and self.start_min < other.start_min)

class RoomType(Enum):
    CLASSROOM = auto()
    LAB = auto()

@dataclass(frozen=True)
class Room:
    id: str
    name: str
    capacity: int
    type: RoomType

@dataclass(frozen=True)
class Instructor:
    id: str
    name: str
    availability: List[TimeSlot]  # time slots the instructor CAN teach

    def is_available(self, ts: TimeSlot) -> bool:
        # Available if any available slot overlaps the given slot
        return any(av.day == ts.day and not (av.end_min <= ts.start_min or ts.end_min <= av.start_min)
                   for av in self.availability)

@dataclass(frozen=True)
class Course:
    id: str
    name: str
    year: int                   # 1..4
    department: str             # e.g., "CENG" or "SENG"
    is_elective: bool
    theory_hours_per_week: int  # e.g., 2 or 3
    lab_hours_per_week: int     # e.g., 0, 1, 2
    instructor_id: str
    enrolled_students: int

class SessionType(Enum):
    THEORY = auto()
    LAB = auto()

@dataclass(frozen=True)
class Session:
    """A single unit we must place on the timetable (commonly 1 hour)."""
    course_id: str
    session_type: SessionType
    duration_hours: int  # typically 1 per slot; consecutive labs are encouraged via a soft rule
    index: int           # ordinal index among sessions of the same type

@dataclass(frozen=True)
class Assignment:
    """A candidate placement for a session."""
    session: Session
    timeslot: TimeSlot
    room_id: str

@dataclass
class Schedule:
    """Holds committed assignments and provides quick indices for conflict checks."""
    assignments: List[Assignment] = field(default_factory=list)
    by_instructor_day: Dict[Tuple[str, DayOfWeek], List[Assignment]] = field(default_factory=dict)
    by_room_day: Dict[Tuple[str, DayOfWeek], List[Assignment]] = field(default_factory=dict)
    by_course: Dict[str, List[Assignment]] = field(default_factory=dict)

    def add(self, assignment: Assignment, course_to_instructor: Dict[str, str]):
        self.assignments.append(assignment)
        course_id = assignment.session.course_id
        instructor_id = course_to_instructor[course_id]
        key_instr = (instructor_id, assignment.timeslot.day)
        key_room = (assignment.room_id, assignment.timeslot.day)

        self.by_instructor_day.setdefault(key_instr, []).append(assignment)
        self.by_room_day.setdefault(key_room, []).append(assignment)
        self.by_course.setdefault(course_id, []).append(assignment)

    def remove(self, assignment: Assignment, course_to_instructor: Dict[str, str]):
        self.assignments.remove(assignment)
        course_id = assignment.session.course_id
        instructor_id = course_to_instructor[course_id]
        key_instr = (instructor_id, assignment.timeslot.day)
        key_room = (assignment.room_id, assignment.timeslot.day)

        self.by_instructor_day[key_instr].remove(assignment)
        self.by_room_day[key_room].remove(assignment)
        self.by_course[course_id].remove(assignment)
