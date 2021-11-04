package example.wjma.spring.model;

import java.io.Serializable;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Person implements Serializable {
	
	private static final long serialVersionUID = 2445247993956960711L;
	
	private int id;
	private String name;
	private Integer age;
	private String sex;
	private List<Preference> preferences;

}
