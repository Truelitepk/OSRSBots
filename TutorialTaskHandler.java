package tasks.tutorial;

import antiban.AntibanManager;
import framework.Task;
import org.dreambot.api.utilities.Logger;

import java.util.ArrayList;
import java.util.List;

public class TutorialTaskHandler {

    private final AntibanManager antibanManager;
    private final List<Task> tasks;

    public TutorialTaskHandler(AntibanManager antibanManager) {
        this.antibanManager = antibanManager;
        this.tasks = new ArrayList<>();

        loadTasks();
    }

    private void loadTasks() {
        tasks.add(new CharacterCreationTask(antibanManager));
        tasks.add(new IntroGuideTask(antibanManager));
        tasks.add(new SurvivalExpertTask(antibanManager));
        tasks.add(new CookingInstructorTask(antibanManager));
        tasks.add(new QuestGuideTask(antibanManager));
        tasks.add(new MiningInstructorTask(antibanManager));
        tasks.add(new CombatInstructorTask(antibanManager));
        tasks.add(new BankingInstructorTask(antibanManager));
        tasks.add(new AccountManagementTask(antibanManager));
        tasks.add(new PrayerInstructorTask(antibanManager));
        tasks.add(new MagicInstructorTask(antibanManager));
    }

    public int execute() {
        if (antibanManager != null) {
            antibanManager.tick();
        }

        for (Task task : tasks) {
            if (task.accept()) {
                Logger.log("[TutorialHandler] Running task: " + task.getName());
                return task.execute();
            }
        }

        return 300;
    }
}
