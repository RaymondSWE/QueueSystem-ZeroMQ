package com.example.server.service;

import com.example.server.models.Student;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@Service
public class QueueService {

    private static final Logger logger = LoggerFactory.getLogger(QueueService.class);

    private final LinkedList<Student> queue = new LinkedList<>();

    public void manageStudent(String name, String clientId) {
        Student existingStudent = queue.stream()
                .filter(s -> s.getName().equals(name))
                .findFirst()
                .orElse(null);

        if (existingStudent == null) {
            List<String> clientIds = new ArrayList<>();
            clientIds.add(clientId);
            Student newStudent = new Student(name, clientIds);
            addStudent(newStudent);
        } else if(!existingStudent.getClientIds().contains(clientId)) {
            existingStudent.getClientIds().add(clientId);
        }
    }

    private void addStudent(Student student) {
        if(!queue.contains(student)) {
            queue.add(student);
            logger.info("Student added: " + student.getName());
        }
    }
    public void removeStudentByName(String name) {
        queue.removeIf(student -> student.getName().equals(name));
        logger.info("Student removed: " + name);
    }

    // Handle inactvice users dont know how though, function should be below.

    public LinkedList<Student> getQueue() {
        return new LinkedList<>(queue);
    }
}

