// Tek dosyalık KidTask örneği (Swing GUI + basit CSV saklama)
// Çalıştırma: javac KidTaskSingle.java && java KidTaskSingle

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class KidTaskSingle extends JFrame {
    // ---- Model ----
    static class Task {
        final String id;
        String title;
        String description;
        LocalDate dueDate;
        int points;
        boolean completed;
        int rating; // 1-5

        Task(String id, String title, String description, LocalDate dueDate, int points) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.dueDate = dueDate;
            this.points = points;
        }

        int awardedPoints() {
            return completed ? points : 0;
        }
    }

    static class Wish {
        final String id;
        String title;
        String description;
        int requiredLevel;
        boolean approved;

        Wish(String id, String title, String description, int requiredLevel) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.requiredLevel = requiredLevel;
        }
    }

    // ---- Storage (CSV) ----
    static class FileStorage {
        private final Path tasksFile;
        private final Path wishesFile;

        FileStorage(Path baseDir) {
            this.tasksFile = baseDir.resolve("tasks.csv");
            this.wishesFile = baseDir.resolve("wishes.csv");
        }

        List<Task> loadTasks() throws IOException {
            if (!Files.exists(tasksFile)) return new ArrayList<>();
            List<Task> tasks = new ArrayList<>();
            for (String line : Files.readAllLines(tasksFile)) {
                if (line.isBlank()) continue;
                String[] p = line.split(",", -1);
                if (p.length < 6) continue;
                Task t = new Task(p[0], p[1], p[2], LocalDate.parse(p[3]), Integer.parseInt(p[4]));
                t.completed = Boolean.parseBoolean(p[5]);
                if (p.length > 6 && !p[6].isBlank()) t.rating = Integer.parseInt(p[6]);
                tasks.add(t);
            }
            return tasks;
        }

        List<Wish> loadWishes() throws IOException {
            if (!Files.exists(wishesFile)) return new ArrayList<>();
            List<Wish> wishes = new ArrayList<>();
            for (String line : Files.readAllLines(wishesFile)) {
                if (line.isBlank()) continue;
                String[] p = line.split(",", -1);
                if (p.length < 5) continue;
                Wish w = new Wish(p[0], p[1], p[2], Integer.parseInt(p[3]));
                w.approved = Boolean.parseBoolean(p[4]);
                wishes.add(w);
            }
            return wishes;
        }

        void saveTasks(List<Task> tasks) throws IOException {
            List<String> lines = new ArrayList<>();
            for (Task t : tasks) {
                lines.add(String.join(",",
                        t.id,
                        t.title,
                        t.description,
                        t.dueDate.toString(),
                        Integer.toString(t.points),
                        Boolean.toString(t.completed),
                        Integer.toString(t.rating)
                ));
            }
            Files.createDirectories(tasksFile.getParent());
            Files.write(tasksFile, lines);
        }

        void saveWishes(List<Wish> wishes) throws IOException {
            List<String> lines = new ArrayList<>();
            for (Wish w : wishes) {
                lines.add(String.join(",",
                        w.id,
                        w.title,
                        w.description,
                        Integer.toString(w.requiredLevel),
                        Boolean.toString(w.approved)
                ));
            }
            Files.createDirectories(wishesFile.getParent());
            Files.write(wishesFile, lines);
        }

        static String newId() {
            return UUID.randomUUID().toString();
        }
    }

    // ---- Service ----
    static class KidTaskService {
        private final FileStorage storage;
        final List<Task> tasks;
        final List<Wish> wishes;

        KidTaskService(FileStorage storage) throws IOException {
            this.storage = storage;
            this.tasks = new ArrayList<>(storage.loadTasks());
            this.wishes = new ArrayList<>(storage.loadWishes());
        }

        Task addTask(String title, String desc, LocalDate due, int points) throws IOException {
            Task t = new Task(FileStorage.newId(), title, desc, due, points);
            tasks.add(t);
            storage.saveTasks(tasks);
            return t;
        }

        void completeTask(String id) throws IOException {
            tasks.stream().filter(t -> t.id.equals(id)).findFirst().ifPresent(t -> {
                t.completed = true;
                if (t.rating == 0) t.rating = 3;
            });
            storage.saveTasks(tasks);
        }

        void rateTask(String id, int rating) throws IOException {
            tasks.stream().filter(t -> t.id.equals(id)).findFirst().ifPresent(t -> t.rating = rating);
            storage.saveTasks(tasks);
        }

        Wish addWish(String title, String desc, int reqLevel) throws IOException {
            Wish w = new Wish(FileStorage.newId(), title, desc, reqLevel);
            wishes.add(w);
            storage.saveWishes(wishes);
            return w;
        }

        void approveWish(String id) throws IOException {
            wishes.stream().filter(w -> w.id.equals(id)).findFirst().ifPresent(w -> w.approved = true);
            storage.saveWishes(wishes);
        }

        int totalPoints() {
            return tasks.stream().mapToInt(Task::awardedPoints).sum();
        }

        int level() {
            double avg = tasks.stream().filter(t -> t.rating > 0).mapToInt(t -> t.rating).average().orElse(1.0);
            return (int) Math.max(1, Math.round(avg));
        }
    }

    // ---- UI ----
    private final KidTaskService service;
    private final DefaultTableModel taskModel;
    private final DefaultTableModel wishModel;
    private final JLabel pointsLabel;
    private final JProgressBar levelBar;

    public KidTaskSingle() throws IOException {
        super("KidTask (Single File)");
        this.service = new KidTaskService(new FileStorage(Path.of("kidtask-data")));

        this.taskModel = new DefaultTableModel(new String[]{"ID", "Title", "Due", "Points", "Done"}, 0);
        this.wishModel = new DefaultTableModel(new String[]{"ID", "Title", "Level", "Approved"}, 0);
        this.pointsLabel = new JLabel();
        this.levelBar = new JProgressBar(1, 5);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.add("Tasks", buildTaskPanel());
        tabs.add("Wishes", buildWishPanel());
        tabs.add("Progress", buildProgressPanel());
        add(tabs, BorderLayout.CENTER);

        refreshTables();
        refreshProgress();
    }

    private JPanel buildTaskPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JTable table = new JTable(taskModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridLayout(2, 5, 8, 8));
        JTextField titleField = new JTextField();
        JTextField descField = new JTextField();
        JTextField dueField = new JTextField("2025-12-31");
        JTextField pointsField = new JTextField("10");

        JButton addBtn = new JButton("Add Task");
        addBtn.addActionListener(_ -> {
            try {
                service.addTask(
                        titleField.getText(),
                        descField.getText(),
                        LocalDate.parse(dueField.getText()),
                        Integer.parseInt(pointsField.getText())
                );
                refreshTables();
            } catch (Exception ex) {
                showError(ex);
            }
        });

        JButton completeBtn = new JButton("Complete");
        completeBtn.addActionListener(_ -> {
            int row = table.getSelectedRow();
            if (row == -1) return;
            String id = table.getValueAt(row, 0).toString();
            try {
                service.completeTask(id);
                refreshTables();
                refreshProgress();
            } catch (Exception ex) {
                showError(ex);
            }
        });

        form.add(new JLabel("Title"));
        form.add(new JLabel("Description"));
        form.add(new JLabel("Due (YYYY-MM-DD)"));
        form.add(new JLabel("Points"));
        form.add(new JLabel(""));

        form.add(titleField);
        form.add(descField);
        form.add(dueField);
        form.add(pointsField);
        form.add(addBtn);

        panel.add(form, BorderLayout.NORTH);
        panel.add(completeBtn, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildWishPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JTable table = new JTable(wishModel);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel form = new JPanel(new GridLayout(2, 4, 8, 8));
        JTextField titleField = new JTextField();
        JTextField descField = new JTextField();
        JTextField levelField = new JTextField("1");
        JButton addBtn = new JButton("Add Wish");
        addBtn.addActionListener(_ -> {
            try {
                service.addWish(
                        titleField.getText(),
                        descField.getText(),
                        Integer.parseInt(levelField.getText())
                );
                refreshTables();
            } catch (Exception ex) {
                showError(ex);
            }
        });

        JButton approveBtn = new JButton("Approve");
        approveBtn.addActionListener(_ -> {
            int row = table.getSelectedRow();
            if (row == -1) return;
            String id = table.getValueAt(row, 0).toString();
            try {
                service.approveWish(id);
                refreshTables();
            } catch (Exception ex) {
                showError(ex);
            }
        });

        form.add(new JLabel("Title"));
        form.add(new JLabel("Description"));
        form.add(new JLabel("Required Level"));
        form.add(new JLabel(""));

        form.add(titleField);
        form.add(descField);
        form.add(levelField);
        form.add(addBtn);

        panel.add(form, BorderLayout.NORTH);
        panel.add(approveBtn, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildProgressPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        pointsLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        levelBar.setAlignmentX(Component.CENTER_ALIGNMENT);
        levelBar.setStringPainted(true);
        panel.add(Box.createVerticalStrut(20));
        panel.add(pointsLabel);
        panel.add(Box.createVerticalStrut(12));
        panel.add(levelBar);
        return panel;
    }

    private void refreshTables() {
        taskModel.setRowCount(0);
        for (Task t : service.tasks) {
            taskModel.addRow(new Object[]{t.id, t.title, t.dueDate, t.points, t.completed});
        }

        wishModel.setRowCount(0);
        for (Wish w : service.wishes) {
            wishModel.addRow(new Object[]{w.id, w.title, w.requiredLevel, w.approved});
        }
    }

    private void refreshProgress() {
        pointsLabel.setText("Total Points: " + service.totalPoints());
        int level = service.level();
        levelBar.setValue(level);
        levelBar.setString("Level " + level);
    }

    private void showError(Exception ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                new KidTaskSingle().setVisible(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}

