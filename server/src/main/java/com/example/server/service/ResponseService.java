package com.example.server.service;

import com.example.server.models.Student;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.zeromq.ZMQ.Socket;

import java.util.ArrayList;
import java.util.List;

@Service
public class ResponseService implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ResponseService.class);
    private static final String PROCESSED_RESPONSE = "{\"message\": \"Your processed response\"}";

    @Autowired
    private Socket zmqResponseSocket;
    @Autowired
    private QueueService queueService;

    private volatile boolean keepRunning = true;

    @Override
    public void run() {
        handleClientRequest();
    }

    public void handleClientRequest() {
        while (keepRunning) {
            String clientRequest = zmqResponseSocket.recvStr();
            logger.info("Received request from client: {}", clientRequest);

            String response = processRequest(clientRequest);
            logger.info("Sending response to client: {}", response);

            zmqResponseSocket.send(response);
        }
    }

    private String processRequest(String request) {
        try {
            JSONObject json = new JSONObject(request);
            String name = json.getString("name");
            String clientId = json.getString("clientId");

            // Check if student already exists in the queue
            Student existingStudent = queueService.getQueue().stream()
                    .filter(s -> s.getName().equals(name))
                    .findFirst()
                    .orElse(null);

            if (existingStudent == null) {
                // If student is not in the queue, add them with the new clientId
                List<String> clientIds = new ArrayList<>();
                clientIds.add(clientId);
                Student newStudent = new Student(name, clientIds);
                queueService.addStudent(newStudent);
            } else {
                // If student is already in the queue, just add the new clientId
                if(!existingStudent.getClientIds().contains(clientId)) {
                    existingStudent.getClientIds().add(clientId);
                }
            }

            logger.info("Current queue: {}", queueService.getQueue());

        } catch (JSONException e) {
            logger.error("Error parsing client request.", e);
        }
        return PROCESSED_RESPONSE;
    }

    public void stop() {
        this.keepRunning = false;
    }
}
