import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;

public class Main {
    public static void main(String[] args) throws FileNotFoundException, XMLStreamException {
        XMLInputFactory streamFactory = XMLInputFactory.newInstance();
        XMLStreamReader reader = streamFactory.createXMLStreamReader(new FileInputStream("people.xml"));

        Map<String, Person> persons_by_id = new HashMap<>();
        Map<String, Person> persons_by_name = new HashMap<>();

        Person currentPerson = null;
        for (; reader.hasNext(); reader.next()) {
            int eventType = reader.getEventType();
            switch (eventType) {
                case XMLStreamConstants.START_ELEMENT -> {
                    switch (reader.getLocalName()) {
                        case "people" -> {
                            // util tag
                            assert reader.getAttributeCount() == 1 && reader.getAttributeLocalName(0).equals("count");
                            System.out.println("num of people: " + reader.getAttributeValue(0));
                        }
                        case "person" -> {
                            // we should create local person for now. if we get id/name, merge with existent
                            currentPerson = new Person();
                            assert reader.getAttributeCount() <= 1;
                            if (reader.getAttributeCount() == 1) {
                                String attribute = reader.getAttributeLocalName(0);
                                assert attribute.equals("id") || attribute.equals("name");
                                if (attribute.equals("name")) {
                                    var name = Arrays.stream(reader.getAttributeValue(0).split(" ")).filter((x) -> x.length() > 0).toList();
                                    currentPerson.firstname = name.get(0);
                                    currentPerson.surname = name.get(1);
                                    currentPerson = getPerson(currentPerson.firstname + currentPerson.surname, persons_by_name, currentPerson);
                                } else {
                                    currentPerson.id = reader.getAttributeValue(0).trim();
                                    currentPerson = getPerson(currentPerson.id, persons_by_id, currentPerson);
                                }
                            }
                        }
                        case "id" -> {
                            assert currentPerson != null;
                            assert reader.getAttributeCount() == 1 && reader.getAttributeLocalName(0).equals("value");
                            currentPerson.id = reader.getAttributeValue(0).trim();
                            currentPerson = getPerson(currentPerson.id, persons_by_id, currentPerson);
                        }
                        case "firstname", "first" -> {
                            assert currentPerson != null;
                            if (reader.getAttributeCount() == 0) {
                                reader.next();
                                assert reader.getEventType() == XMLStreamConstants.CHARACTERS;
                                currentPerson.firstname = reader.getText().trim();
                            } else {
                                assert reader.getAttributeCount() == 1 && reader.getAttributeLocalName(0).equals("value");
                                currentPerson.firstname = reader.getAttributeValue(0).trim();
                            }
                            if (currentPerson.surname != null) {
                                currentPerson = getPerson(currentPerson.firstname + currentPerson.surname, persons_by_name, currentPerson);
                            }
                        }
                        case "surname", "family", "family-name" -> {
                            assert currentPerson != null;
                            if (reader.getAttributeCount() == 0) {
                                assert reader.getLocalName().equals("family") || reader.getLocalName().equals("family-name");
                                reader.next();
                                assert reader.getEventType() == XMLStreamConstants.CHARACTERS;
                                currentPerson.surname = reader.getText().trim();
                            } else {
                                assert reader.getAttributeCount() == 1 && reader.getAttributeLocalName(0).equals("value");
                                currentPerson.surname = reader.getAttributeValue(0).trim();
                            }
                            if (currentPerson.firstname != null) {
                                currentPerson = getPerson(currentPerson.firstname + currentPerson.surname, persons_by_name, currentPerson);
                            }
                        }
                        case "fullname", "children" -> {
                            assert currentPerson != null;
                            assert reader.getAttributeCount() == 0;
                        }
                        case "gender" -> {
                            assert currentPerson != null;
                            if (reader.getAttributeCount() == 0) {
                                reader.next();
                                assert reader.getEventType() == XMLStreamConstants.CHARACTERS;
                                currentPerson.add_property("gender", reader.getText().trim().toUpperCase().substring(0, 1));
                            } else {
                                assert reader.getAttributeCount() == 1 && reader.getAttributeLocalName(0).equals("value");
                                currentPerson.add_property("gender", reader.getAttributeValue(0).trim().toUpperCase().substring(0, 1));
                            }
                        }
                        case "spouce" -> {
                            assert currentPerson != null;
                            if (reader.getAttributeCount() > 0) {
                                assert reader.getAttributeCount() == 1 && reader.getAttributeLocalName(0).equals("value");
                                if (!reader.getAttributeValue(0).trim().equals("NONE")) {
                                    var name = Arrays.stream(reader.getAttributeValue(0).split(" "))
                                            .filter((x) -> x.length() > 0).toList();
                                    currentPerson.add_property("spouce", name.get(0) + " " + name.get(1));
                                }
                            }
                            if (reader.hasText()) {
                                if (!reader.getText().trim().equals("NONE")) {
                                    var name = Arrays.stream(reader.getText().split(" "))
                                            .filter((x) -> x.length() > 0).toList();
                                    currentPerson.add_property("spouce", name.get(0) + " " + name.get(1));
                                }
                            }
                        }
                        case "siblings" -> {
                            assert currentPerson != null;
                            if (reader.getAttributeCount() != 0) {
                                assert reader.getAttributeCount() == 1 && reader.getAttributeLocalName(0).equals("val");
                                List<String> siblings = Arrays.stream(reader.getAttributeValue(0).split(" ")).filter((x) -> x.length() > 0).toList();
                                for (var s : siblings) {
                                    currentPerson.add_property("siblings", s);
                                }
                            }
                        }
                        case "brother", "sister", "child", "father", "mother" -> {
                            assert currentPerson != null;
                            String prop = reader.getLocalName();
                            reader.next();
                            var name = Arrays.stream(reader.getText().split(" "))
                                    .filter((x) -> x.length() > 0).toList();
                            currentPerson.add_property(prop, name.get(0) + " " + name.get(1));
                        }
                        case "siblings-number", "children-number" -> {
                            assert currentPerson != null;
                            assert reader.getAttributeCount() == 1 && reader.getAttributeLocalName(0).equals("value");
                            if (reader.getLocalName().equals("siblings-number"))
                                currentPerson.siblings_number = Integer.parseInt(reader.getAttributeValue(0).trim());
                            else
                                currentPerson.children_number = Integer.parseInt(reader.getAttributeValue(0).trim());
                        }
                        case "son", "daughter" -> {
                            assert currentPerson != null;
                            assert reader.getAttributeCount() == 1 && reader.getAttributeLocalName(0).equals("id");
                            currentPerson.add_property(reader.getLocalName(), reader.getAttributeValue(0).trim());
                        }
                        case "parent" -> {
                            assert currentPerson != null;
                            if (reader.getAttributeCount() == 0) {
                                reader.next();
                                if (!reader.getText().trim().equals("UNKNOWN")) {
                                    currentPerson.add_property("parent", reader.getText().trim());
                                }
                            } else {
                                assert reader.getAttributeCount() == 1 && reader.getAttributeLocalName(0).equals("value");
                                if (!reader.getAttributeValue(0).trim().equals("UNKNOWN")) {
                                    currentPerson.add_property("parent", reader.getAttributeValue(0).trim());
                                }
                            }
                        }
                        case "wife", "husband" -> {
                            assert reader.getAttributeCount() == 1 && reader.getAttributeLocalName(0).equals("value");
                            assert currentPerson != null;
                            currentPerson.add_property(reader.getLocalName(), reader.getAttributeValue(0));
                        }
                        default -> System.out.println("Unknown property: " + reader.getLocalName());
                    }
                }
                case XMLStreamConstants.END_ELEMENT -> {
                    if (reader.getLocalName().equals("person")) {
                        assert currentPerson != null;
                        assert currentPerson.id != null || (currentPerson.firstname != null && currentPerson.surname != null);
                        if (currentPerson.id != null) {
                            persons_by_id.putIfAbsent(currentPerson.id, currentPerson);
                        }
                        if (currentPerson.firstname != null && currentPerson.surname != null) {
                            persons_by_name.putIfAbsent(currentPerson.firstname + currentPerson.surname, currentPerson);
                        }
                        currentPerson = null;
                    }
                }
            }
        }

        System.out.println("number of persons with unique id: " + persons_by_id.size());
        for (Person p : persons_by_id.values()) {
            assert p.firstname != null && p.surname != null;
            Set<String> children_ids = new HashSet<>();
            var daughters = p.get_property("daughter");
            if (daughters != null) {
                for (var d : daughters) {
                    assert d.matches("P[0-9]*");
                    children_ids.add(d);
                }
            }
            var sons = p.get_property("son");
            if (sons != null) {
                for (var s : sons) {
                    assert s.matches("P[0-9]*");
                    children_ids.add(s);
                }
            }
            var child = p.get_property("child");
            if (child != null) {
                for (var c : child) {
                    if (c.matches("P[0-9]*")) {
                        children_ids.add(c);
                    } else {
                        var name = Arrays.stream(c.split(" ")).toList();
                        var id_by_name = persons_by_name.get(name.get(0) + name.get(1)).id;
                        assert id_by_name.matches("P[0-9]*");
                        children_ids.add(id_by_name);
                    }
                }
            }
            assert (children_ids.size() == p.children_number) || p.children_number == -1;
        }
    }

    private static Person getPerson(String key, Map<String, Person> persons, Person currentPerson) {
        Person hit = persons.get(key);
        if(hit != null){
            // let's collect local data...
            // аххахаххаха у нас еще и полные тезки есть
            if(hit.id != null && currentPerson.id != null && !hit.id.equals(currentPerson.id))
                return currentPerson;
            for(var k : currentPerson.properties.keySet()){
                for(var p : currentPerson.get_property(k)) {
                    hit.add_property(k, p);
                }
            }
            assert hit.firstname == null || currentPerson.firstname == null || hit.firstname.equals(currentPerson.firstname);
            assert hit.surname == null || currentPerson.surname == null || hit.surname.equals(currentPerson.surname);
            if(hit.firstname == null && currentPerson.firstname != null){
                hit.firstname = currentPerson.firstname;
            }
            if(hit.surname == null && currentPerson.surname != null){
                hit.surname = currentPerson.surname;
            }
            if(hit.id == null && currentPerson.id != null){
                hit.id = currentPerson.id;
            }
            currentPerson = hit;
        }
        return currentPerson;
    }
}
