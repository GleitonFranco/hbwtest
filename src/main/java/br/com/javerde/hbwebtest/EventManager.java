package br.com.javerde.hbwebtest;

import java.util.Date;
import java.util.List;

import org.hibernate.Session;

public class EventManager {

	public static void main(String[] args) {
		EventManager mgr = new EventManager();
		if (args[0].equals("store") && args.length>1) {
			mgr.createAndStoreEvent("Another Event", new Date());
		} else if (args[0].equals("list")) {
			List events = mgr.listEvents();
			for (int i = 0; i < events.size(); i++) {
				Event theEvent = (Event) events.get(i);
				System.out.println("Event: " + theEvent.getTitle() + " Time: " + theEvent.getDate());
			}
		} else if (args[0].equals("createperson") && args.length>2) {
			mgr.createAndStorePerson(args[1],args[2]);
		}else if (args[0].equals("addpersontoevent") && args.length>3) {
            Long personId = mgr.createAndStorePerson(args[1], args[2]);
            Long eventId = mgr.createAndStoreEvent(args[3], new Date());
            mgr.addPersonToEvent(personId, eventId);
            System.out.println("Added person " + personId + " to event " + eventId);
        }
		HibernateUtil.getSessionFactory().close();
	}

	private Long createAndStorePerson(String firstname, String lastname) {
		Session session = HibernateUtil.getSessionFactory().getCurrentSession();
		session.beginTransaction();
		Person person = new Person(firstname, lastname);
		Event e = (Event)session.save(person);
		session.getTransaction().commit();
		return e.getId();
	}

	private Long createAndStoreEvent(String title, Date theDate) {
		Session session = HibernateUtil.getSessionFactory().getCurrentSession();
		session.beginTransaction();

		Event theEvent = new Event();
		theEvent.setTitle(title);
		theEvent.setDate(theDate);
		Event e = (Event)session.save(theEvent);

		session.getTransaction().commit();
		return e.getId();
	}

	private List listEvents() {
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();
        List result = session.createQuery("from Event").list();// HQL
        session.getTransaction().commit();
        return result;
    }
	
	private void addPersonToEvent(Long personId, Long eventId) {
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();

        Person aPerson = (Person) session.load(Person.class, personId);
        Event anEvent = (Event) session.load(Event.class, eventId);
        aPerson.getEvents().add(anEvent);

        session.getTransaction().commit();// automatic dirty checking: Hibernate automatically detects that the collection has been modified
// persistent state: bound to a particular Hibernate org.hibernate.Session
// As long as they are in persistent state, Hibernate monitors any changes and executes SQL
// flushing : The process of synchronizing the memory state with the database, at the end of a unit of work (no caso o commit())        
    }
	
	private void addPersonToEvent2(Long personId, Long eventId) {
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();

        Person aPerson = (Person) session
                .createQuery("select p from Person p left join fetch p.events where p.id = :pid")
                .setParameter("pid", personId)
                .uniqueResult(); // Eager fetch the collection so we can use it detached
        Event anEvent = (Event) session.load(Event.class, eventId);

        session.getTransaction().commit();

        // End of first unit of work

        aPerson.getEvents().add(anEvent); // aPerson (and its collection) is detached

        // Begin second unit of work

        Session session2 = HibernateUtil.getSessionFactory().getCurrentSession();
        session2.beginTransaction();
        session2.update(aPerson); // Reattachment of aPerson

        session2.getTransaction().commit();
    }
	
	private void addEmailToPerson(Long personId, String emailAddress) {
        Session session = HibernateUtil.getSessionFactory().getCurrentSession();
        session.beginTransaction();

        Person aPerson = (Person) session.load(Person.class, personId);
        // adding to the emailAddress collection might trigger a lazy load of the collection
        aPerson.getEmailAddresses().add(emailAddress);

        session.getTransaction().commit();
    }

}
//mvn exec:java -Dexec.mainClass="br.com.javerde.hbwebtest.EventManager" -Dexec.args="store um-evento"
// mvn exec:java -Dexec.mainClass="br.com.javerde.hbwebtest.EventManager" -Dexec.args="list"