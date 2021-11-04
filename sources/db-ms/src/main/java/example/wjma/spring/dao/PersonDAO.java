package example.wjma.spring.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import example.wjma.spring.model.Person;

@Repository
public interface PersonDAO extends JpaRepository<Person, Integer>{
}
