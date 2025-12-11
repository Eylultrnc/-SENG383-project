
# beeplan/io.py
from __future__ import annotations
import json
from typing import Dict, List

from .models import (
    DayOfWeek, RoomType, SessionType,
    TimeSlot, Room, Instructor, Course
)

# --- JSON helpers (simple, explicit mappings) ---
def load_timeslots(json_list: List[dict]) -> List[TimeSlot]:
    return [TimeSlot(day=DayOfWeek[item["day"]], start=item["start"], end=item["end"]) for item in json_list]

def load_rooms(json_list: List[dict]) -> List[Room]:
    return [Room(id=item["id"], name=item["name"], capacity=item["capacity"], type=RoomType[item["type"]]) for item in json_list]

def load_instructors(json_list: List[dict]) -> List[Instructor]:
    # availability must be List[dict] with {day, start, end}
    def _ts(tsd): return TimeSlot(day=DayOfWeek[tsd["day"]], start=tsd["start"], end=tsd["end"])
    return [
        Instructor(
            id=item["id"],
            name=item["name"],
            availability=[_ts(ts) for ts in item.get("availability", [])]
        )
        for item in json_list
    ]

def load_courses(json_list: List[dict]) -> List[Course]:
    return [
        Course(
            id=item["id"],
            name=item["name"],
            year=item["year"],
            department=item["department"],
            is_elective=item["is_elective"],
            theory_hours_per_week=item["theory_hours_per_week"],
            lab_hours_per_week=item["lab_hours_per_week"],
            instructor_id=item["instructor_id"],
            enrolled_students=item["enrolled_students"],
        )
        for item in json_list
    ]

def dumps_schedule(schedule) -> str:
    """Serialize assignments for inspection or storage."""
    data = []
    for a in schedule.assignments:
        data.append({
            "course_id": a.session.course_id,
            "type": a.session.session_type.name,
            "index": a.session.index,
            "duration_hours": a.session.duration_hours,
            "day": a.timeslot.day.name,
            "start": a.timeslot.start,
            "end": a.timeslot.end,
            "room_id": a.room_id
        })
    return json.dumps(data, indent=2)
