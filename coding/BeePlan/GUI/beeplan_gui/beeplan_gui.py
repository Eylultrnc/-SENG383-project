import sys
import json
import csv
from collections import defaultdict
from dataclasses import dataclass, field
from typing import List, Dict, Optional, Tuple

from PyQt5.QtWidgets import (
    QApplication, QMainWindow, QWidget, QVBoxLayout, QHBoxLayout, QLabel, QPushButton,
    QTableWidget, QTableWidgetItem, QComboBox, QFileDialog, QMessageBox, QStackedWidget,
    QLineEdit, QSpinBox, QCheckBox, QTextEdit
)
from PyQt5.QtCore import Qt


# -------------------------------
# Data models
# -------------------------------
DAYS = ["Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma"]
HOURS = [
    "08:30-09:20", "09:30-10:20", "10:30-11:20", "11:30-12:20",
    "12:30-13:20", "13:30-14:20", "14:30-15:20", "15:30-16:20",
    "16:30-17:20"
]

FRIDAY_BLOCKED = {"13:30-14:20", "14:30-15:20"}  # 13:20–15:10 aralığına karşılık

@dataclass
class Room:
    name: str
    capacity: int
    type: str  # "classroom" or "lab"

@dataclass
class Course:
    code: str
    name: str
    department: str  # e.g., "CENG" or "SENG"
    year: int        # 1..4
    mandatory: bool
    theory_hours: int
    lab_hours: int
    instructor: str
    is_elective: bool = False

@dataclass
class InstructorAvailability:
    instructor: str
    not_available: Dict[str, List[str]] = field(default_factory=lambda: defaultdict(list))

@dataclass
class ScheduledSlot:
    day: str
    hour: str
    room: str
    kind: str  # "theory" or "lab"
    course_code: str

@dataclass
class ScheduleResult:
    slots: List[ScheduledSlot]
    violations: List[str]


# -------------------------------
# Simple heuristic scheduler
# -------------------------------
class Scheduler:
    def __init__(
        self,
        courses: List[Course],
        availability: Dict[str, InstructorAvailability],
        rooms: List[Room]
    ):
        self.courses = courses
        self.availability = availability
        self.rooms = rooms

        # Index rooms
        self.classrooms = [r for r in rooms if r.type == "classroom"]
        self.labs = [r for r in rooms if r.type == "lab"]

        # Grid: day -> hour -> list of slots (room dimension)
        self.grid: Dict[str, Dict[str, List[ScheduledSlot]]] = {
            d: {h: [] for h in HOURS} for d in DAYS
        }

        # Track instructor loads per day (theory hours)
        self.instructor_daily_theory: Dict[str, Dict[str, int]] = defaultdict(lambda: defaultdict(int))

        # Track year-wise electives to avoid overlap
        self.elective_slots_by_dept: Dict[str, set] = defaultdict(set)  # e.g., {"CENG": {(day,hour), ...}}

        # Violations
        self.violations: List[str] = []

    def is_available(self, instructor: str, day: str, hour: str) -> bool:
        if day == "Cuma" and hour in FRIDAY_BLOCKED:
            return False
        av = self.availability.get(instructor)
        if av and hour in av.not_available.get(day, []):
            return False
        return True

    def room_free(self, day: str, hour: str, kind: str) -> Optional[str]:
        # prefer type-appropriate room
        candidates = self.classrooms if kind == "theory" else self.labs
        for r in candidates:
            occupied = any(s.room == r.name for s in self.grid[day][hour])
            if not occupied:
                return r.name
        # If none free, try any room (still record violation)
        for r in self.rooms:
            occupied = any(s.room == r.name for s in self.grid[day][hour])
            if not occupied:
                self.violations.append(f"Oda tipi uygun değil: {day} {hour} {r.name} ({kind})")
                return r.name
        return None

    def place_block(self, course: Course, day: str, start_idx: int, length: int, kind: str) -> bool:
        # ensure consecutive hours and feasibility
        if start_idx + length > len(HOURS):
            return False
        hours = HOURS[start_idx:start_idx+length]
        # rule: Friday blocked
        if day == "Cuma" and any(h in FRIDAY_BLOCKED for h in hours):
            return False
        # availability + room + collisions
        for h in hours:
            if not self.is_available(course.instructor, day, h):
                return False
            # instructor overlap (same hour)
            if any(s.course_code and s.course_code != course.code for s in self.grid[day][h] if s.kind in ("theory", "lab")):
                # not strictly needed (room-level), but keep simple
                pass
        # instructor daily theory limit (for theory)
        if kind == "theory":
            if self.instructor_daily_theory[course.instructor][day] + length > 4:
                return False

        # room allocation
        rooms_used = []
        for h in hours:
            room = self.room_free(day, h, kind)
            if room is None:
                return False
            rooms_used.append(room)

        # commit
        for idx, h in enumerate(hours):
            self.grid[day][h].append(ScheduledSlot(day, h, rooms_used[idx], kind, course.code))
        if kind == "theory":
            self.instructor_daily_theory[course.instructor][day] += length
        # electives non-overlap (CENG/SENG)
        if course.is_elective:
            self.elective_slots_by_dept[course.department].update((day, h) for h in hours)
        return True

    def electives_overlap_violation(self):
        # CENG and SENG electives must not overlap
        overlap = self.elective_slots_by_dept["CENG"].intersection(self.elective_slots_by_dept["SENG"])
        for (d, h) in overlap:
            self.violations.append(f"Seçmeli çakışması: CENG & SENG {d} {h}")

    def theory_then_lab(self, course: Course) -> bool:
        # lab must follow theory; prefer 2 consecutive lab hours
        if course.lab_hours == 0:
            return True
        # search for theory placement first then lab immediately after
        for d in DAYS:
            for start_t in range(len(HOURS)):
                if self.place_block(course, d, start_t, course.theory_hours, "theory"):
                    # try lab immediately after
                    lab_start = start_t + course.theory_hours
                    # optional preference: 2 consecutive lab hours if possible
                    lab_len = course.lab_hours
                    if self.place_block(course, d, lab_start, lab_len, "lab"):
                        # lab capacity check
                        for h in HOURS[lab_start:lab_start+lab_len]:
                            for s in self.grid[d][h]:
                                if s.course_code == course.code and s.kind == "lab":
                                    room_obj = next((r for r in self.rooms if r.name == s.room), None)
                                    if room_obj and room_obj.capacity > 40:
                                        # capacity violation: lab capacity must be ≤ 40
                                        self.violations.append(f"Lab kapasitesi ihlali: {room_obj.name} {room_obj.capacity} > 40 (kurs {course.code})")
                        return True
                    else:
                        # rollback theory and continue search
                        self.rollback(course)
        return False

    def rollback(self, course: Course):
        for d in DAYS:
            for h in HOURS:
                before = len(self.grid[d][h])
                self.grid[d][h] = [s for s in self.grid[d][h] if s.course_code != course.code]
                after = len(self.grid[d][h])
                if before != after:
                    # adjust instructor load if theory slot removed
                    self.instructor_daily_theory[course.instructor][d] = max(
                        0,
                        self.instructor_daily_theory[course.instructor][d] - 1
                    )

    def schedule(self) -> ScheduleResult:
        # order heuristic: mandatory first, higher year first, then electives
        ordered = sorted(self.courses, key=lambda c: (not c.mandatory, -c.year, c.is_elective))

        for c in ordered:
            ok = self.theory_then_lab(c)
            if not ok:
                self.violations.append(f"Yerleştirilemedi: {c.code} ({c.name})")

        # cross-check: 3rd year courses should not overlap with electives
        third_year_slots = set()
        elective_slots = set()
        for d in DAYS:
            for h in HOURS:
                for s in self.grid[d][h]:
                    crs = next((x for x in self.courses if x.code == s.course_code), None)
                    if not crs:
                        continue
                    if crs.year == 3:
                        third_year_slots.add((d, h))
                    if crs.is_elective:
                        elective_slots.add((d, h))
        for (d, h) in third_year_slots.intersection(elective_slots):
            self.violations.append(f"3.sınıf–seçmeli çakışması: {d} {h}")

        # electives department overlap
        self.electives_overlap_violation()

        # build final list
        slots = []
        for d in DAYS:
            for h in HOURS:
                slots.extend(self.grid[d][h])

        return ScheduleResult(slots=slots, violations=self.violations)


# -------------------------------
# GUI
# -------------------------------
class Dashboard(QWidget):
    def __init__(self, on_import, on_export, on_generate, on_view_timetable, on_view_report):
        super().__init__()
        layout = QVBoxLayout()
        title = QLabel("🐝 BeePlan – Çankaya Univ. Course Scheduling")
        title.setAlignment(Qt.AlignCenter)
        title.setStyleSheet("font-size: 22px; font-weight: bold; color: #222;")
        subtitle = QLabel("Verileri içe aktarın, programı oluşturun, ihlalleri görüntüleyin.")
        subtitle.setAlignment(Qt.AlignCenter)
        subtitle.setStyleSheet("color: #555;")

        btns = QHBoxLayout()
        import_btn = QPushButton("📥 Import (JSON/CSV)")
        export_btn = QPushButton("📤 Export (JSON)")
        gen_btn = QPushButton("⚙️ Generate Schedule")
        view_btn = QPushButton("📅 View Timetable")
        report_btn = QPushButton("🧾 View Report")

        for b in (import_btn, export_btn, gen_btn, view_btn, report_btn):
            b.setStyleSheet("padding:10px; border-radius:6px;")
        import_btn.clicked.connect(on_import)
        export_btn.clicked.connect(on_export)
        gen_btn.clicked.connect(on_generate)
        view_btn.clicked.connect(on_view_timetable)
        report_btn.clicked.connect(on_view_report)

        btns.addWidget(import_btn)
        btns.addWidget(export_btn)
        btns.addWidget(gen_btn)
        btns.addWidget(view_btn)
        btns.addWidget(report_btn)

        layout.addWidget(title)
        layout.addWidget(subtitle)
        layout.addLayout(btns)
        layout.addStretch()
        self.setLayout(layout)


class TimetableView(QWidget):
    def __init__(self, on_back):
        super().__init__()
        self.on_back = on_back
        layout = QVBoxLayout()
        title = QLabel("📅 Haftalık Program")
        title.setAlignment(Qt.AlignCenter)
        title.setStyleSheet("font-size:18px; font-weight:bold;")
        self.table = QTableWidget()
        self.table.setRowCount(len(HOURS))
        self.table.setColumnCount(len(DAYS) + 1)
        self.table.setHorizontalHeaderLabels(["Saat"] + DAYS)
        self.table.horizontalHeader().setStretchLastSection(True)

        back = QPushButton("⬅ Dashboard")
        back.clicked.connect(on_back)

        layout.addWidget(title)
        layout.addWidget(self.table)
        layout.addWidget(back)
        self.setLayout(layout)

    def color_cell(self, item: QTableWidgetItem, kind: str, violation: bool):
        if violation:
            item.setBackground(Qt.red)
        else:
            item.setBackground(Qt.yellow if kind == "lab" else Qt.white)

    def render(self, slots: List[ScheduledSlot], courses: List[Course], violations: List[str]):
        # clear
        for r in range(len(HOURS)):
            for c in range(len(DAYS)+1):
                self.table.setItem(r, c, None)

        # first column: hours
        for r, h in enumerate(HOURS):
            self.table.setItem(r, 0, QTableWidgetItem(h))

        # build map day-hour -> text
        cell_map: Dict[Tuple[str, str], List[str]] = defaultdict(list)
        viol_set = set(violations)
        for s in slots:
            crs = next((x for x in courses if x.code == s.course_code), None)
            if not crs:
                continue
            label = f"{s.course_code} ({'T' if s.kind=='theory' else 'L'})\n{crs.instructor}\n{s.room}"
            cell_map[(s.day, s.hour)].append(label)

        for d_idx, day in enumerate(DAYS, start=1):
            for r_idx, hour in enumerate(HOURS):
                texts = cell_map.get((day, hour), [])
                item = QTableWidgetItem("\n---\n".join(texts))
                # violation visual if more than one course in same cell or Friday block used
                violation = (len(texts) > 1) or (day == "Cuma" and hour in FRIDAY_BLOCKED)
                kind = "lab" if any("(L)" in t for t in texts) else "theory"
                self.color_cell(item, kind, violation)
                self.table.setItem(r_idx, d_idx, item)


class ReportView(QWidget):
    def __init__(self, on_back):
        super().__init__()
        layout = QVBoxLayout()
        title = QLabel("🧾 İhlal Raporu")
        title.setAlignment(Qt.AlignCenter)
        title.setStyleSheet("font-size:18px; font-weight:bold;")
        self.text = QTextEdit()
        self.text.setReadOnly(True)
        back = QPushButton("⬅ Dashboard")
        back.clicked.connect(on_back)
        layout.addWidget(title)
        layout.addWidget(self.text)
        layout.addWidget(back)
        self.setLayout(layout)

    def render(self, violations: List[str]):
        if not violations:
            self.text.setText("İhlal bulunamadı. ✔️")
        else:
            self.text.setText("\n".join(f"- {v}" for v in violations))


class ManualInputView(QWidget):
    def __init__(self, on_add_course, on_add_room, on_add_availability, on_back):
        super().__init__()
        layout = QVBoxLayout()
        title = QLabel("✍️ Veri Girişi")
        title.setAlignment(Qt.AlignCenter)
        title.setStyleSheet("font-size:18px; font-weight:bold;")
        layout.addWidget(title)

        # Course form
        layout.addWidget(QLabel("Ders Ekle"))
        self.code = QLineEdit(); self.code.setPlaceholderText("Kod (MAT101)")
        self.name = QLineEdit(); self.name.setPlaceholderText("Ad (Matematik)")
        self.dept = QLineEdit(); self.dept.setPlaceholderText("Bölüm (CENG/SENG)")
        self.year = QSpinBox(); self.year.setRange(1,4)
        self.mandatory = QCheckBox("Zorunlu")
        self.theory = QSpinBox(); self.theory.setRange(0,6); self.theory.setValue(3)
        self.lab = QSpinBox(); self.lab.setRange(0,4)
        self.instructor = QLineEdit(); self.instructor.setPlaceholderText("Öğretim Elemanı")
        self.elective = QCheckBox("Seçmeli")
        add_course_btn = QPushButton("Dersi Ekle")
        add_course_btn.clicked.connect(lambda: on_add_course(
            Course(
                code=self.code.text(), name=self.name.text(), department=self.dept.text(),
                year=self.year.value(), mandatory=self.mandatory.isChecked(),
                theory_hours=self.theory.value(), lab_hours=self.lab.value(),
                instructor=self.instructor.text(), is_elective=self.elective.isChecked()
            )
        ))

        layout.addWidget(self.code); layout.addWidget(self.name); layout.addWidget(self.dept)
        layout.addWidget(QLabel("Sınıf (1-4):")); layout.addWidget(self.year)
        layout.addWidget(self.mandatory)
        layout.addWidget(QLabel("Teori saat:")); layout.addWidget(self.theory)
        layout.addWidget(QLabel("Lab saat:")); layout.addWidget(self.lab)
        layout.addWidget(self.instructor); layout.addWidget(self.elective)
        layout.addWidget(add_course_btn)

        # Room form
        layout.addWidget(QLabel("Derslik/Lab Ekle"))
        self.room_name = QLineEdit(); self.room_name.setPlaceholderText("Oda (A101)")
        self.room_capacity = QSpinBox(); self.room_capacity.setRange(10,200); self.room_capacity.setValue(40)
        self.room_type = QComboBox(); self.room_type.addItems(["classroom", "lab"])
        add_room_btn = QPushButton("Odayı Ekle")
        add_room_btn.clicked.connect(lambda: on_add_room(Room(
            name=self.room_name.text(), capacity=self.room_capacity.value(), type=self.room_type.currentText()
        )))
        layout.addWidget(self.room_name); layout.addWidget(self.room_capacity); layout.addWidget(self.room_type); layout.addWidget(add_room_btn)

        # Availability form
        layout.addWidget(QLabel("Hoca Müsaitlik (Müsait değil)"))
        self.av_inst = QLineEdit(); self.av_inst.setPlaceholderText("Öğretim Elemanı")
        self.av_day = QComboBox(); self.av_day.addItems(DAYS)
        self.av_hour = QComboBox(); self.av_hour.addItems(HOURS)
        add_av_btn = QPushButton("Takvim Ekle")
        add_av_btn.clicked.connect(lambda: on_add_availability(self.av_inst.text(), self.av_day.currentText(), self.av_hour.currentText()))
        layout.addWidget(self.av_inst); layout.addWidget(self.av_day); layout.addWidget(self.av_hour); layout.addWidget(add_av_btn)

        back = QPushButton("⬅ Dashboard")
        back.clicked.connect(on_back)
        layout.addWidget(back)
        layout.addStretch()
        self.setLayout(layout)


class MainWindow(QMainWindow):
    def __init__(self):
        super().__init__()
        self.setWindowTitle("BeePlan Scheduler")
        self.setGeometry(120, 120, 1100, 700)

        # Data stores
        self.courses: List[Course] = []
        self.rooms: List[Room] = []
        self.availability: Dict[str, InstructorAvailability] = {}
        self.last_result: Optional[ScheduleResult] = None

        # Default rooms
        self.rooms.extend([
            Room("A101", 40, "classroom"),
            Room("A102", 30, "classroom"),
            Room("L201", 40, "lab"),
            Room("L202", 35, "lab"),
        ])

        # Views
        self.stack = QStackedWidget()
        self.setCentralWidget(self.stack)

        self.dashboard = Dashboard(
            on_import=self.import_data,
            on_export=self.export_data,
            on_generate=self.generate_schedule,
            on_view_timetable=self.view_timetable,
            on_view_report=self.view_report
        )
        self.timetable = TimetableView(self.go_dashboard)
        self.report = ReportView(self.go_dashboard)
        self.manual = ManualInputView(
            on_add_course=self.add_course,
            on_add_room=self.add_room,
            on_add_availability=self.add_availability,
            on_back=self.go_dashboard
        )

        self.stack.addWidget(self.dashboard)
        self.stack.addWidget(self.timetable)
        self.stack.addWidget(self.report)
        self.stack.addWidget(self.manual)

        # Top bar buttons
        top = QWidget()
        top_layout = QHBoxLayout()
        top_layout.setContentsMargins(10, 5, 10, 5)
        top_layout.setSpacing(8)
        btn_manual = QPushButton("✍️ Veri Girişi")
        btn_manual.clicked.connect(self.view_manual)
        btn_gen = QPushButton("⚙️ Generate")
        btn_gen.clicked.connect(self.generate_schedule)
        btn_view = QPushButton("📅 Timetable")
        btn_view.clicked.connect(self.view_timetable)
        btn_report = QPushButton("🧾 Report")
        btn_report.clicked.connect(self.view_report)
        for b in (btn_manual, btn_gen, btn_view, btn_report):
            b.setStyleSheet("padding:6px 10px;")
        top_layout.addWidget(btn_manual)
        top_layout.addWidget(btn_gen)
        top_layout.addWidget(btn_view)
        top_layout.addWidget(btn_report)
        top.setLayout(top_layout)
        self.addToolBarBreak()
        self.addToolBar(Qt.TopToolBarArea, self._toolbar_from_widget(top))

        # Style
        self.setStyleSheet("""
            QMainWindow { background: #ffffff; }
            QLabel { color: #333; }
            QPushButton { border-radius:6px; }
            QTableWidget::item { padding:6px; }
            QHeaderView::section { background:#f5f5f5; padding:6px; border:1px solid #ddd; }
        """)

    def _toolbar_from_widget(self, w: QWidget):
        from PyQt5.QtWidgets import QToolBar
        tb = QToolBar()
        tb.addWidget(w)
        return tb

    # Navigation
    def go_dashboard(self):
        self.stack.setCurrentWidget(self.dashboard)

    def view_timetable(self):
        if not self.last_result:
            QMessageBox.information(self, "Bilgi", "Önce programı oluşturun.")
            return
        self.timetable.render(self.last_result.slots, self.courses, self.last_result.violations)
        self.stack.setCurrentWidget(self.timetable)

    def view_report(self):
        if not self.last_result:
            QMessageBox.information(self, "Bilgi", "Önce programı oluşturun.")
            return
        self.report.render(self.last_result.violations)
        self.stack.setCurrentWidget(self.report)

    def view_manual(self):
        self.stack.setCurrentWidget(self.manual)

    # Data actions
    def add_course(self, course: Course):
        if not course.code or not course.name or not course.instructor or not course.department:
            QMessageBox.warning(self, "Eksik", "Kod, ad, bölüm ve öğretim elemanı zorunlu.")
            return
        self.courses.append(course)
        QMessageBox.information(self, "Eklendi", f"{course.code} – {course.name}")

    def add_room(self, room: Room):
        if not room.name:
            QMessageBox.warning(self, "Eksik", "Oda adı gerekli.")
            return
        self.rooms.append(room)
        QMessageBox.information(self, "Eklendi", f"Oda: {room.name} ({room.type}, {room.capacity})")

    def add_availability(self, instructor: str, day: str, hour: str):
        if not instructor:
            QMessageBox.warning(self, "Eksik", "Öğretim elemanı gerekli.")
            return
        av = self.availability.get(instructor)
        if not av:
            av = InstructorAvailability(instructor=instructor)
            self.availability[instructor] = av
        av.not_available[day].append(hour)
        QMessageBox.information(self, "Eklendi", f"{instructor} müsait değil: {day} {hour}")

    def import_data(self):
        path, _ = QFileDialog.getOpenFileName(self, "Import JSON/CSV", "", "JSON (*.json);;CSV (*.csv)")
        if not path:
            return
        try:
            if path.endswith(".json"):
                with open(path, "r", encoding="utf-8") as f:
                    data = json.load(f)
                self.courses = [Course(**c) for c in data.get("courses", [])]
                self.rooms = [Room(**r) for r in data.get("rooms", [])]
                self.availability = {}
                for a in data.get("availability", []):
                    av = InstructorAvailability(instructor=a["instructor"])
                    for k, v in a.get("not_available", {}).items():
                        av.not_available[k] = v
                    self.availability[av.instructor] = av
            else:
                # CSV: courses only (simple schema)
                with open(path, newline="", encoding="utf-8") as f:
                    reader = csv.DictReader(f)
                    self.courses = []
                    for row in reader:
                        self.courses.append(Course(
                            code=row["code"], name=row["name"], department=row["department"],
                            year=int(row["year"]), mandatory=row["mandatory"].lower()=="true",
                            theory_hours=int(row["theory_hours"]), lab_hours=int(row["lab_hours"]),
                            instructor=row["instructor"], is_elective=row.get("is_elective","").lower()=="true"
                        ))
            QMessageBox.information(self, "Import", "Veriler içe aktarıldı.")
        except Exception as e:
            QMessageBox.critical(self, "Hata", f"İçe aktarma başarısız: {e}")

    def export_data(self):
        path, _ = QFileDialog.getSaveFileName(self, "Export JSON", "", "JSON (*.json)")
        if not path:
            return
        try:
            data = {
                "courses": [c.__dict__ for c in self.courses],
                "rooms": [r.__dict__ for r in self.rooms],
                "availability": [
                    {"instructor": a.instructor, "not_available": a.not_available}
                    for a in self.availability.values()
                ],
                "result": {
                    "slots": [s.__dict__ for s in (self.last_result.slots if self.last_result else [])],
                    "violations": (self.last_result.violations if self.last_result else [])
                }
            }
            with open(path, "w", encoding="utf-8") as f:
                json.dump(data, f, ensure_ascii=False, indent=2)
            QMessageBox.information(self, "Export", "JSON olarak dışa aktarıldı.")
        except Exception as e:
            QMessageBox.critical(self, "Hata", f"Dışa aktarma başarısız: {e}")

    def generate_schedule(self):
        if not self.courses:
            QMessageBox.warning(self, "Uyarı", "Önce dersleri ekleyin veya import yapın.")
            return
        if not self.rooms:
            QMessageBox.warning(self, "Uyarı", "En az bir oda ekleyin.")
            return
        scheduler = Scheduler(self.courses, self.availability, self.rooms)
        self.last_result = scheduler.schedule()
        if self.last_result.violations:
            QMessageBox.information(self, "Tamamlandı", f"Program oluşturuldu. {len(self.last_result.violations)} ihlal var.")
        else:
            QMessageBox.information(self, "Tamamlandı", "Program oluşturuldu. İhlal yok.")
        self.view_timetable()


# -------------------------------
# Entry
# -------------------------------
if __name__ == "__main__":
    app = QApplication(sys.argv)
    w = MainWindow()
    w.show()
    sys.exit(app.exec_())
