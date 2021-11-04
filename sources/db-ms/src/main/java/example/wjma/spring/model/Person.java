package example.wjma.spring.model;

import java.io.Serializable;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "personas")
public class Person implements Serializable {
	
	private static final long serialVersionUID = 2445247993956960711L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;
	
	@Column(name = "nombre", nullable = false, length = 50)
	private String name;
	
	@Column(name = "edad", nullable = false, length = 2)
	private Integer age;

	@Column(name = "sexo", nullable = false, length = 1)
	private String sex;

	@OneToMany(fetch=FetchType.LAZY, cascade = CascadeType.ALL, mappedBy="person")
	private List<Preference> preferences;

}
