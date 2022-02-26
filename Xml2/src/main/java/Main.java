import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.xml.sax.SAXException;
import ru.nsu.fit.people.People;
import ru.nsu.fit.people.PersonGenderType;
import ru.nsu.fit.people.PersonReferenceType;
import ru.nsu.fit.people.PersonType;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.validation.SchemaFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.function.Predicate;

public class Main {
    public static void main(String[] args) throws XMLStreamException, FileNotFoundException {
        ArrayList<Person> records = parseRecords();
        ArrayList<Person> tmp_records = new ArrayList<>();

        // инициализируем базу: подбираем все уникальные айди, при совпадении сливаем
        Map<String, Person> persons_by_id = new HashMap<>();
        for (Person record : records) {
            if (record.id != null) {
                if (persons_by_id.containsKey(record.id)) {
                    var hit = persons_by_id.get(record.id);
                    mergePerson(hit, record);
                } else {
                    persons_by_id.put(record.id, record);
                }
            } else {
                tmp_records.add(record);
            }
        }
        records = tmp_records;
        tmp_records = new ArrayList<>();
        System.out.println("number of persons with unique id: " + persons_by_id.size());


        // проверяем, что все записи содержат имя и фамилию после первого прохода
        assert persons_by_id.values().stream().allMatch(x -> x.firstname != null && x.surname != null);
        assert records.stream().allMatch(x -> x.firstname != null && x.surname != null);

        // используем записи с именем-фамилией, для которых нет тезок в базе
        for (Person record : records) {
            // проверяем, что в базе нет тезок (записей с одинаковыми именами+фамилиями, но разными айди)
            var matched_persons =
                    persons_by_id
                            .values()
                            .parallelStream()
                            .filter(x -> x.firstname.equals(record.firstname) && x.surname.equals(record.surname)).toList();
               if(matched_persons.size() == 1) {
                   // успех: в базе ровно один человек с таким ФИ, сливаем
                   mergePerson(matched_persons.get(0), record);
               }
               else {
                   tmp_records.add(record);
               }
        }
        records = tmp_records;
        tmp_records = new ArrayList<>();

        assert records.size() == 38;

        // замыкания
        String[][] cljs = {{"siblings", "siblings"}, {"wife", "husband"}, {"husband", "wife"}, {"son", "parent"}, {"daughter", "parent"}};
        for(var clj : cljs) {
            for (Person person : persons_by_id.values()) {
                var base = person.get_property(clj[0]);
                if (base != null) {
                    for (var id : base.stream().toList()) {
                        assert id.matches("P[0-9]*");
                        persons_by_id.get(id).add_property(clj[1], person.id);
                    }
                }
            }
        }

        // заменим все имена родственников на айди, где это возможно
        names_to_ids(persons_by_id.values().stream().toList(), persons_by_id.values().stream().toList());
        names_to_ids(records, persons_by_id.values().stream().toList());

        // фильтруем людей в базе по имени и фамилии
        // дальше мы можем их сравнивать по другим признакам
        String[] heuristics = {"siblings", "husband", "wife", "parent", "daughter", "son", "mother", "father"};
        for(var e : heuristics) {
            for (Person record : records) {
                if (record.get_property(e) != null) {
                    Set<String> record_set = record.get_property(e);
                    assert record_set.stream().allMatch(x -> x.matches("P[0-9]*"));
                    var matched_persons =
                            persons_by_id
                                    .values()
                                    .parallelStream()
                                    .filter(x -> {
                                                Set<String> intersection = new HashSet<>(record_set);
                                                if (x.get_property(e) == null)
                                                    return false;
                                                var x_set = x.get_property(e);
                                                intersection.retainAll(x_set);
                                                return x.firstname.equals(record.firstname) && x.surname.equals(record.surname) &&
                                                        !intersection.isEmpty();
                                            }
                                    ).toList();
                    if (matched_persons.size() == 1) {
                        mergePerson(matched_persons.get(0), record);
                    } else {
                        tmp_records.add(record);
                    }
                } else {
                    tmp_records.add(record);
                }
            }
            records = tmp_records;
            tmp_records = new ArrayList<>();
        }


        for(var record : records) {
            boolean used = use_heuristic(persons_by_id, record, "daughter", x -> x.firstname.equals(record.firstname) && x.surname.equals(record.surname) && (x.children_number != 0));
            used |= use_heuristic(persons_by_id, record, "wife",
                    x -> x.firstname.equals(record.firstname) && x.surname.equals(record.surname) &&
                            ((x.get_property("gender") == null) || (x.get_property("gender") != null && !x.get_property("gender").stream().toList().get(0).equals("F"))));
            used |= use_heuristic(persons_by_id, record, "husband",
                    x -> x.firstname.equals(record.firstname) && x.surname.equals(record.surname) &&
                            ((x.get_property("gender") == null) || (x.get_property("gender") != null && !x.get_property("gender").stream().toList().get(0).equals("M"))));
            used |= use_heuristic(persons_by_id, record, "brother",
                    x -> x.firstname.equals(record.firstname) && x.surname.equals(record.surname) &&
                            x.get_property("siblings") != null && x.get_property("siblings").contains(record.get_property("brother").stream().toList().get(0)));
            used |= use_heuristic(persons_by_id, record, "mother",
                    x -> x.firstname.equals(record.firstname) && x.surname.equals(record.surname) &&
                            x.get_property("mother") != null && x.get_property("mother").contains(record.get_property("mother").stream().toList().get(0)));
            used |= use_heuristic(persons_by_id, record, "wife",
                    x -> x.firstname.equals(record.firstname) && x.surname.equals(record.surname) &&
                            persons_by_id.get(record.get_property("wife").stream().toList().get(0)).children_number == 1 &&
                            x.children_number == 1);
            if (!used) {
                tmp_records.add(record);
            }
        }
        records = tmp_records;
        tmp_records = new ArrayList<>();


        for(Person record : records){
            if(record.properties.size() == 0){
                    continue;
            }
            tmp_records.add(record);
        }
        records = tmp_records;

        System.out.println("unused info: " + records.size());
        for(var r : records){
            System.out.println(r);
        }


        String[][] gender_by_def = {{"husband", "M"}, {"wife", "F"},
                                    {"sister", "F"}, {"brother", "M"},
                                    {"father", "M"}, {"mother", "F"},
                                    {"daughter", "F"}, {"son", "M"}};
        for(Person person : persons_by_id.values()){
            for(var e : gender_by_def) {
                var base = person.get_property(e[0]);
                if (base != null) {
                    for (var id : base.stream().toList()) {
                        if(id.matches("P[0-9]*")) {
                            persons_by_id.get(id).add_property("gender", e[1]);
                        }
                    }
                }
            }
        }
        assert persons_by_id.values().stream().allMatch(x -> x.get_property("gender") != null);

        String[] troubled_tags = {"father", "mother", "child", "brother", "sister", "spouce"};
        int cnt = 0;
        for(Person person : persons_by_id.values()){
            for(var tag : troubled_tags){
                if(person.get_property(tag) == null)
                    continue;
                Set<String> updated = new HashSet<>();
                for(var name : person.properties.get(tag)){
                    if(name.matches("P[0-9]*")){
                        updated.add(name);
                    }
                    else{
                        var real_name = Arrays.stream(name.split(" ")).filter((x) -> x.length() > 0).toList();
                        assert real_name.size() == 2;
                        var matched_persons =
                                persons_by_id
                                        .values()
                                        .parallelStream()
                                        .filter(x -> x.firstname.equals(real_name.get(0)) && x.surname.equals(real_name.get(1)) && !x.id.equals(person.id)).toList();
                        if(matched_persons.size() == 1) {
                            updated.add(matched_persons.get(0).id);
                        }
                        else{
                            // попытаемся сматчить по-другому
                            if(tag.equals("mother") || tag.equals("father")){
                                var matched_by_tag = matched_persons.stream().filter(x -> (
                                        (x.get_property("son") != null && x.get_property("son").contains(person.id)) ||
                                        (x.get_property("daughter") != null && x.get_property("daughter").contains(person.id)) ||
                                                (x.get_property("child") != null && x.get_property("child").contains(person.id))

                                )).toList();
                                cnt = getCnt(cnt, tag, updated, matched_persons, matched_by_tag, person);
                            }
                            else if(tag.equals("child")){
                                var matched_by_tag = matched_persons.stream().filter(x -> (
                                        (x.get_property("father") != null && x.get_property("father").contains(person.id)) ||
                                                (x.get_property("mother") != null && x.get_property("mother").contains(person.id)) ||
                                                (x.get_property("parent") != null && x.get_property("parent").contains(person.id))
                                )).toList();
                                cnt = getCnt(cnt, tag, updated, matched_persons, matched_by_tag, person);
                            }
                            else if(tag.equals("brother") || tag.equals("sister")){
                                var matched_by_tag = matched_persons.stream().filter(x ->
                                        (x.get_property("brother") != null && x.get_property("brother").contains(person.id)) ||
                                                (x.get_property("sister") != null && x.get_property("sister").contains(person.id)) ||
                                                (x.get_property("siblings") != null && x.get_property("siblings").contains(person.id))
                                ).toList();
                                cnt = getCnt(cnt, tag, updated, matched_persons, matched_by_tag, person);
                            }
                            else if(tag.equals("spouce")){
                                var matched_by_tag = matched_persons.stream().filter(x ->
                                        (x.get_property("wife") != null && x.get_property("wife").contains(person.id)) ||
                                                (x.get_property("husband") != null && x.get_property("husband").contains(person.id)) ||
                                                (x.get_property("spouce") != null && x.get_property("spouce").contains(person.id))
                                ).toList();
                                cnt = getCnt(cnt, tag, updated, matched_persons, matched_by_tag, person);
                            }
                            else {
                                System.out.println("Problem with " + tag + ", id: " + person.id);
                                for(var guy : matched_persons){
                                    System.out.println(guy);
                                }
                                cnt++;
                            }
                            assert matched_persons.size() == 2;
                        }
                    }
                }
                person.properties.put(tag, updated);
            }
        }
        assert cnt == 0;
        assert persons_by_id.values().stream().allMatch(x -> x.properties.values().stream().allMatch(y->y.stream().allMatch(z -> z.equals("M") || z.equals("F") || z.matches("P[0-9]*"))));

        // удалим всех parent, spouce, siblings и children потому что мы теперь можем
        for(var person : persons_by_id.values()){
           if(person.get_property("parent") != null){
              for(var parent : person.get_property("parent")){
                 assert parent.matches("P[0-9]*");
                 if(persons_by_id.get(parent).get_property("gender").stream().toList().get(0).equals("F")){
                    person.add_property("mother", parent);
                 }
                 else{
                    person.add_property("father", parent);
                 }
              }
           }
            if(person.get_property("child") != null){
                for(var parent : person.get_property("child")){
                    assert parent.matches("P[0-9]*");
                    if(persons_by_id.get(parent).get_property("gender").stream().toList().get(0).equals("F")){
                        person.add_property("daughter", parent);
                    }
                    else{
                        assert persons_by_id.get(parent).get_property("gender").stream().toList().get(0).equals("M");
                        person.add_property("son", parent);
                    }
                }
            }
            if(person.get_property("spouce") != null){
                for(var parent : person.get_property("spouce")){
                    assert parent.matches("P[0-9]*");
                    if(persons_by_id.get(parent).get_property("gender").stream().toList().get(0).equals("F")){
                        person.add_property("wife", parent);
                    }
                    else{
                        assert persons_by_id.get(parent).get_property("gender").stream().toList().get(0).equals("M");
                        person.add_property("husband", parent);
                    }
                }
            }
            if(person.get_property("siblings") != null){
                for(var parent : person.get_property("siblings")){
                    assert parent.matches("P[0-9]*");
                    if(persons_by_id.get(parent).get_property("gender").stream().toList().get(0).equals("F")){
                        person.add_property("sister", parent);
                    }
                    else{
                        assert persons_by_id.get(parent).get_property("gender").stream().toList().get(0).equals("M");
                        person.add_property("brother", parent);
                    }
                }
            }
        }

        validate(persons_by_id);

        People people = collect_people(persons_by_id);
        write_xml(people);
        System.out.println("written successfully");


    }
    private static void write_xml(People people){
        try {
            ClassLoader classLoader = People.class.getClassLoader();
            JAXBContext jc = JAXBContext.newInstance("ru.nsu.fit.people", classLoader);
            Marshaller writer = jc.createMarshaller();
            SchemaFactory schemaFactory =
                    SchemaFactory.newInstance(javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI);
            File schemaFile = new File("schema.xsd");
            writer.setSchema(schemaFactory.newSchema(schemaFile));
            writer.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            writer.marshal(people, new File("output.xml"));
        } catch (JAXBException | SAXException e) {
            e.printStackTrace();
        }
    }
    private static People collect_people(Map<String, Person> persons_by_id){
        People people = new People();
        for(var person : persons_by_id.values()){
            PersonType person_to_write = new PersonType();
            person_to_write.setId(person.id);
            person_to_write.setPersonFirstname(person.firstname);
            person_to_write.setPersonSurname(person.surname);
            person_to_write.setPersonGender(person.get_property("gender").stream().toList().get(0).equals("F") ? PersonGenderType.FEMALE : PersonGenderType.MALE);
            people.getPerson().add(person_to_write);
        }
        for(var person : people.getPerson()){
            Person old_person = persons_by_id.get(person.getId());
            // единичные ребята
            if(old_person.get_property("father") != null) {
                String id = old_person.get_property("father").stream().toList().get(0);
                var new_person_type = people.getPerson().stream().filter(x -> x.getId().equals(id)).toList().get(0);
                var ref = new PersonReferenceType();
                ref.setPersonId(new_person_type);
                person.setFather(ref);
            }
            if(old_person.get_property("mother") != null) {
                String id = old_person.get_property("mother").stream().toList().get(0);
                var new_person_type = people.getPerson().stream().filter(x -> x.getId().equals(id)).toList().get(0);
                var ref = new PersonReferenceType();
                ref.setPersonId(new_person_type);
                person.setMother(ref);
            }
            if(old_person.get_property("husband") != null) {
                String id = old_person.get_property("husband").stream().toList().get(0);
                var new_person_type = people.getPerson().stream().filter(x -> x.getId().equals(id)).toList().get(0);
                var ref = new PersonReferenceType();
                ref.setPersonId(new_person_type);
                person.setHusband(ref);
            }
            if(old_person.get_property("wife") != null) {
                String id = old_person.get_property("wife").stream().toList().get(0);
                var new_person_type = people.getPerson().stream().filter(x -> x.getId().equals(id)).toList().get(0);
                var ref = new PersonReferenceType();
                ref.setPersonId(new_person_type);
                person.setWife(ref);
            }

            // неограниченные ребята
            if(old_person.get_property("son") != null) {
                var ids = old_person.get_property("son").stream().toList();
                for(var id : ids) {
                    var new_person_type = people.getPerson().stream().filter(x -> x.getId().equals(id)).toList().get(0);
                    var ref = new PersonReferenceType();
                    ref.setPersonId(new_person_type);
                    person.getSon().add(ref);
                }
            }
            if(old_person.get_property("daughter") != null) {
                var ids = old_person.get_property("daughter").stream().toList();
                for(var id : ids) {
                    var new_person_type = people.getPerson().stream().filter(x -> x.getId().equals(id)).toList().get(0);
                    var ref = new PersonReferenceType();
                    ref.setPersonId(new_person_type);
                    person.getDaughter().add(ref);
                }
            }
            if(old_person.get_property("brother") != null) {
                var ids = old_person.get_property("brother").stream().toList();
                for(var id : ids) {
                    var new_person_type = people.getPerson().stream().filter(x -> x.getId().equals(id)).toList().get(0);
                    var ref = new PersonReferenceType();
                    ref.setPersonId(new_person_type);
                    person.getBrother().add(ref);
                }
            }
            if(old_person.get_property("sister") != null) {
                var ids = old_person.get_property("sister").stream().toList();
                for(var id : ids) {
                    var new_person_type = people.getPerson().stream().filter(x -> x.getId().equals(id)).toList().get(0);
                    var ref = new PersonReferenceType();
                    ref.setPersonId(new_person_type);
                    person.getSister().add(ref);
                }
            }

        }
        return people;
    }

    private static void validate(Map<String, Person> persons_by_id) {
        String[][] categories = {{"daughter", "son", "child"}, {"sister", "brother", "siblings"}};
        for(int i = 0 ; i < 2 ; i++){
            for (Person p : persons_by_id.values()) {
                Set<String> ids = new HashSet<>();
                for(var subcat : categories[i]) {
                    var subcats = p.get_property(subcat);
                    if (subcats != null) {
                        for (var d : subcats) {
                            assert d.matches("P[0-9]*");
                            ids.add(d);
                        }
                    }
                }
                if(i == 0) {
                    assert (ids.size() == p.children_number) || p.children_number == -1;
                }
                else {
                    assert (ids.size() == p.siblings_number) || p.siblings_number == -1;

                }
            }
        }
    }

    private static int getCnt(int cnt, String tag, Set<String> updated, List<Person> matched_persons, List<Person> matched_by_tag, Person person) {
        if(matched_by_tag.size() == 1) {
            updated.add(matched_by_tag.get(0).id);
        }
        else {
            System.out.println("Problem with " + tag + ", this guy: " + person);
            for(var guy : matched_persons){
                System.out.println(guy);
            }
            cnt++;
        }
        return cnt;
    }

    private static boolean use_heuristic(Map<String, Person> persons_by_id, Person record, String property, Predicate<Person> p){
        if(record.get_property(property) != null){
            var matched_persons =
                    persons_by_id
                            .values()
                            .parallelStream()
                            .filter(p)
                            .toList();
            if (matched_persons.size() == 1) {
                mergePerson(matched_persons.get(0), record);
                return true;
            }
        }
        return false;
    }

    private static ArrayList<Person> parseRecords() throws FileNotFoundException, XMLStreamException {
        XMLInputFactory streamFactory = XMLInputFactory.newInstance();
        XMLStreamReader reader = streamFactory.createXMLStreamReader(new FileInputStream("people.xml"));

        ArrayList<Person> records = new ArrayList<>();

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
                                    assert name.size() == 2;
                                    currentPerson.firstname = name.get(0);
                                    currentPerson.surname = name.get(1);
                                } else {
                                    currentPerson.id = reader.getAttributeValue(0).trim();
                                }
                            }
                        }
                        case "id" -> {
                            assert currentPerson != null;
                            assert reader.getAttributeCount() == 1 && reader.getAttributeLocalName(0).equals("value");
                            currentPerson.id = reader.getAttributeValue(0).trim();
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
                        records.add(currentPerson);
                        currentPerson = null;
                    }
                }
            }
        }
        return records;
    }

    private static void mergePerson(Person hit, Person record){
        for(var k : record.properties.keySet()){
            for(var p : record.get_property(k)) {
                hit.add_property(k, p);
            }
        }
        if(hit.firstname == null && record.firstname != null){
            hit.firstname = record.firstname;
        }
        if(hit.surname == null && record.surname != null){
            hit.surname = record.surname;
        }
        if(hit.siblings_number == -1 && record.siblings_number != -1){
            hit.siblings_number = record.siblings_number;
        }
        if(hit.children_number == -1 && record.children_number != -1){
            hit.children_number = record.children_number;;
        }
    }
    private static void names_to_ids(List<Person> to_change, List<Person> persons_by_id){
        for(Person person : to_change){
            for(var property_name : person.properties.keySet()){
                if(property_name.equals("gender"))
                    continue;
                Set<String> updated = new HashSet<>();
                for(var property_value : person.properties.get(property_name)){
                    if(property_value.matches("P[0-9]*")){
                        updated.add(property_value);
                    }
                    else{
                        var name = Arrays.stream(property_value.split(" "))
                                .filter((x) -> x.length() > 0).toList();
                        assert name.size() == 2;
                        var matched_persons =
                                persons_by_id
                                        .parallelStream()
                                        .filter(x -> x.firstname.equals(name.get(0)) && x.surname.equals(name.get(1))).toList();
                        if(matched_persons.size() == 1) {
                            // успех, заменяем на айди
                            updated.add(matched_persons.get(0).id);
                        }
                        else{
                            updated.add(property_value);
                            //System.out.println(property_value);
                            assert matched_persons.size() == 2;
                        }
                    }
                }
                person.properties.put(property_name, updated);
            }
        }
    }
}
