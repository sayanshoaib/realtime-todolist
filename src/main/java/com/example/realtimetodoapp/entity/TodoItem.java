package com.example.realtimetodoapp.entity;

import lombok.*;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@Data
@NoArgsConstructor
@RequiredArgsConstructor
@ToString
public class TodoItem {

    @Id
    @GeneratedValue
    private long id;

    @NonNull
    private String text;

    @NonNull
    private String username;

    private boolean isCompleted = false;
}
