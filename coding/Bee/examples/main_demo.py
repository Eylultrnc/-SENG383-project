
# examples/main_demo.py
from beeplan import (
    Scheduler, Course, Instructor, Room, TimeSlot,
    DayOfWeek, RoomType
)

def demo():
    timeslots = [
        TimeSlot(DayOfWeek.MON, "09:00", "10:00"),
        TimeSlot(DayOfWeek.MON, "10:00", "11:00"),
        TimeSlot(DayOfWeek.MON, "11:00", "12:00"),
        TimeSlot(DayOfWeek.WED, "10:00", "11:00"),
        TimeSlot(DayOfWeek.FRI, "13:00", "14:00"),
        TimeSlot(DayOfWeek.FRI, "14:00", "15:00"),
        TimeSlot(DayOfWeek.FRI, "15:10", "16:10"),  # after exam block
    ]

    instructors = [
        Instructor(id="I1", name="Dr. Ada", availability=timeslots),
        Instructor(id="I2", name="Dr. Turing", availability=timeslots),
    ]

    rooms = [
        Room(id="R1", name="C101", capacity=60, type=RoomType.CLASSROOM),
        Room(id="R2", name="C102", capacity=60, type=RoomType.CLASSROOM),
        Room(id="L1", name="Lab A", capacity=40, type=RoomType.LAB),
    ]

    courses = [
        Course(id="CENG201", name="Data Structures", year=2, department="CENG",
               is_elective=False, theory_hours_per_week=3, lab_hours_per_week=2,
               instructor_id="I1", enrolled_students=38),
        Course(id="ELEC401", name="ML Elective", year=4, department="CENG",
               is_elective=True, theory_hours_per_week=3, lab_hours_per_week=0,
               instructor_id="I2", enrolled_students=55),
    ]

    scheduler = Scheduler()
    try:
        schedule = scheduler.generate_schedule(courses, instructors, rooms, timeslots)
        print("Schedule generated. Assignments:")
        for a in schedule.assignments:
            print(f"{a.session.session_type.name:6} {a.session.course_id:8} {a.timeslot.day.name} "
                  f"{a.timeslot.start}-{a.timeslot.end} in {a.room_id}")
        print("\nViolations logged during search (for diagnostics):")
        for v in scheduler.get_report().get("violations", []):
            print("-", v)
    except Exception as e:
        print("Scheduling failed:", e)

if __name__ == "__main__":
    demo()
