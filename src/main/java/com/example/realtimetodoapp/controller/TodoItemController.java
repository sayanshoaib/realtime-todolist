package com.example.realtimetodoapp.controller;

import com.example.realtimetodoapp.entity.TodoItem;
import com.example.realtimetodoapp.service.TodoItemService;
import io.ably.lib.rest.AblyRest;
import io.ably.lib.types.AblyException;
import io.ably.lib.util.JsonUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class TodoItemController {

    @Autowired
    private TodoItemService todoItemService;

    private AblyRest ablyRest;

    public TodoItemController(@Value("${ABLY_API_KEY}") String apiKey) throws AblyException {
        ablyRest = new AblyRest(apiKey);
    }

//    @Value("${eyJ0eXAiOiJKV1QiLCJ2ZXJzaW9uIjoxLCJhbGciOiJIUzI1NiJ9.eyJqdGkiOiJkZmFmYmZiNi03MWMwLTRjMGQtOWJmOS1mNTljMmMxYzE3MGUiLCJpc3MiOiJhYmx5LmNvbSIsImlhdCI6MTY2NTc2NjEwOCwic3ViIjozMDI3MH0.FH8NVfkQnrCcntCOWfGEXwzm1mgVSbJu1f2xKFWNMMA}")
//    private void setAblyRest(String apiKey) throws AblyException {
//        ablyRest = new AblyRest(apiKey);
//    }



    private final String CHANNEL_NAME = "default";

    @GetMapping("/todolist")
    public ResponseEntity<List<TodoItem>> findAll() {
        List<TodoItem> items = todoItemService.findAll();
        return ResponseEntity.ok().body(items);
    }

    @GetMapping("/todolist/{id}")
    public ResponseEntity<TodoItem> findById(@PathVariable("id") Long id) {
        Optional<TodoItem> item = todoItemService.find(id);
        return ResponseEntity.of(item);
    }

    @PostMapping("/todolist")
    public ResponseEntity<TodoItem> createTodolist(
            @CookieValue(value = "username") String username,
            @RequestBody Map<String, String> json) {
        if (json.get("text") == null) {
            return ResponseEntity.badRequest().body(null);
        }

        TodoItem newItem = todoItemService.create(json.get("text"), username);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(newItem.getId())
                .toUri();

        String idInString = Long.toString(newItem.getId());

        JsonUtils.JsonUtilsObject object = io.ably.lib.util.JsonUtils.object();
        object.add("text", json.get("text"));
        object.add("username", username);
        object.add("id", idInString);

        publishToChannel("add", object.toJson());
        return ResponseEntity.created(location).body(newItem);
    }

    @PutMapping("/todolist/{id}/complete")
    public ResponseEntity<TodoItem> todolistCompleted(@PathVariable("id") Long id) {
        Optional<TodoItem> updated = todoItemService.updateCompletionStatus(id, true);

        return updated
                .map(value -> {
                    publishToChannel("complete", Long.toString(id));
                    return ResponseEntity.ok().body(value);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/todolist/{id}/uncomplete")
    public ResponseEntity<TodoItem> todolistUncompleted(@PathVariable("id") Long id) {
        Optional<TodoItem> updated = todoItemService.updateCompletionStatus(id, false);

        return updated
                .map(value -> {
                    publishToChannel("incomplete", Long.toString(id));
                    return ResponseEntity.ok().body(value);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/todolist/{id}")
    public ResponseEntity<TodoItem> deleteTodolist(
            @CookieValue(value = "username") String username,
            @PathVariable("id") Long id) {

        Optional<TodoItem> todoItemOptional = todoItemService.find(id);
        if (todoItemOptional.isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        if (todoItemOptional.get().getUsername().equals(username)) {
            todoItemService.delete(id);
            publishToChannel("remove", Long.toString(id));
        }

        return ResponseEntity.noContent().build();
    }

    /* Publish an update to the TODO list to an Ably Channel */
    private boolean publishToChannel(String name, Object data) {
        try {
            ablyRest.channels.get(CHANNEL_NAME).publish(name, data);
        } catch (AblyException err) {
            System.out.println(err.errorInfo);
            return false;
        }
        return true;
    }
}
