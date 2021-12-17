import java.util.*;

public class Person {
    public String id = null, firstname = null, surname = null;
    public Integer siblings_number = -1, children_number = -1;
    public Map<String, Set<String>> properties = new HashMap<>();
    public Set<String> get_property(String key){
        return properties.getOrDefault(key, null);
    }
    public void add_property(String key, String value){
        if(!properties.containsKey(key)) {
            properties.put(key, new HashSet<>());
        }
        properties.get(key).add(value);
    }
    public String get_real_property(String key){
        return get_property(key).stream().toList().get(0);
    }
}
