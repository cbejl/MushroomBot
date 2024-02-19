package cbejl.mushroom.gsonTranscript;

import cbejl.mushroom.config.BotConfig;
import lombok.Data;

@Data
public class GsonTasks {
    private String title;
    private String columnId;
    private String description;
    private boolean archived;
    private boolean completed;
    private Checklist[] checklists;

    public GsonTasks(String title, String description) {
        this(title, new BotConfig().getColumnRecs(), description, false, false, new Checklist[]{});
    }

    public GsonTasks(String title, String columnId, String description, boolean archived, boolean completed,
                     Checklist[] checklists) {
        this.title = title;
        this.columnId = columnId;
        this.description = description;
        this.archived = archived;
        this.completed = completed;
        this.checklists = checklists;
    }

    @Data
    public static class Checklist {
        Checklist() {

        }
        public Checklist(String title, Item[] items) {
            this.title = title;
            this.items = items;
        }

        private String title;
        private Item[] items;
        @Data
        public static class Item {
            Item() {

            }
            public Item(String title) {
                this(title, false);
            }
            Item(String title, boolean isCompleted) {
                this.title = title;
                this.isCompleted = isCompleted;
            }
            String title;
            boolean isCompleted;
        }
    }

}
