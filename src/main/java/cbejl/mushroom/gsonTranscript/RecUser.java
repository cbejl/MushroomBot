package cbejl.mushroom.gsonTranscript;

import lombok.Data;

@Data
public class RecUser {
    public Content[] content;
    @Data
    public static class Content {
        public String title;
        public String id;
        public boolean completed;
        public String description;
    }
}
