package ly.secore.KeyLoader.Database;

public class Product {
    private String key;
    private String type;
    private String personality;

    public void setKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setPersonality(String personality) {
        this.personality = personality;
    }

    public String getPersonality() {
        return personality;
    }

    @Override
    public String toString() {
        return "Product{" +
                "key='" + key + '\'' +
                ", type='" + type + '\'' +
                ", personality='" + personality + '\'' +
                '}';
    }
}
