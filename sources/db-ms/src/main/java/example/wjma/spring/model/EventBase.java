package example.wjma.spring.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class EventBase<T> {
    
    private List<T> data;
    private String operation;
    private String requestID;
    
    private boolean status;
    private String message;

    private List<T> response;
}