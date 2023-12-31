package com.example.server.worker;

import com.example.server.service.StudentService;
import com.example.server.service.SupervisorService;
import jakarta.annotation.PostConstruct;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.zeromq.ZMQ.Socket;

@Component
public class ResponderWorker implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ResponderWorker.class);

    @Autowired
    private Socket zmqResponseSocket;

    @Autowired
    private StudentService studentService;

    @Autowired
    private SupervisorService supervisorService;

    @Autowired
    private PublisherWorker publisherWorker;
    private final String INVALID_MESSAGE_TYPE = "invalidMessage";
    private volatile boolean keepRunning = true;

    private void handleErrorMessage(String errorType, String message) {
        JSONObject json = new JSONObject();
        json.put("message", message);
        json.put("error", errorType);
        zmqResponseSocket.send(json.toString());
    }

    @Override
    public void run() {
        handleAllRequest();
    }

    @PostConstruct
    public void start() {
        new Thread(this).start();
    }

    public void handleAllRequest() {
        while (keepRunning && !Thread.currentThread().isInterrupted()) {
            try {
                String clientRequest = zmqResponseSocket.recvStr();
                if (clientRequest.trim().startsWith("{")) {
                    JSONObject jsonRequest = new JSONObject(clientRequest);
                    logger.info("Received clientRequest: {}", clientRequest);
                    String type = jsonRequest.optString("type");
                    switch (type) {
                        case "heartbeat" -> handleHeartbeat(jsonRequest);
                        case "supervisor" -> handleSupervisorRequest(jsonRequest);
                        case "startup" -> handleStartupMessage(jsonRequest);
                        default -> handleStudentRequest(jsonRequest);
                    }
                } else {
                    logger.error("Invalid JSON received: {}", clientRequest);
                    handleErrorMessage("InvalidJSON", "Received string is not a valid JSON object.");
                }
            } catch (Exception e) {
                if (!keepRunning) {
                    break;
                }
                logger.error("Error handling client request: ", e);
                handleErrorMessage("serverError", "Error handling client request: " + e.getMessage());
            }
        }
    }


    private void handleHeartbeat(JSONObject jsonRequest) {
        String clientId = jsonRequest.optString("clientId");
        String name = jsonRequest.optString("name");

        if (!clientId.isEmpty() && !name.isEmpty()) {
            logger.info("Received heartbeat from: {} with clientId: {}", name, clientId);
            studentService.updateClientHeartbeat(name);
            zmqResponseSocket.send(new JSONObject().toString());
        } else {
            logger.error("Invalid heartbeat message: clientId or name not found");
            handleErrorMessage("invalidMessage", "Invalid heartbeat message");
        }
    }


    private void handleStartupMessage(JSONObject jsonRequest) {
        if (jsonRequest.has("client_number")) {
            int clientNumber = jsonRequest.optInt("client_number", -1);

            if (clientNumber != -1) {
                logger.info("Received startup message from client number: {}  ", clientNumber);
            } else {
                logger.info("Received startup message from client.");
            }
            publisherWorker.broadcastQueue(studentService.getQueue());
            publisherWorker.broadcastSupervisorsStatus();
            zmqResponseSocket.send("Acknowledged startup");
        } else {
            logger.error("No client id found");
            handleErrorMessage(INVALID_MESSAGE_TYPE, "No client id was found");
        }

    }

    private void handleSupervisorRequest(JSONObject jsonRequest) {
        logger.info("Received Supervisor Request: {}", jsonRequest.toString());

        if (jsonRequest.has("addSupervisor")) {
            handleAddSupervisor(jsonRequest);
        } else if (jsonRequest.has("attendStudent")) {
            handleAttendStudent(jsonRequest);
        } else if (jsonRequest.has("makeAvailable")) {
            handleMakeAvailable(jsonRequest);
        } else {
            handleErrorMessage(INVALID_MESSAGE_TYPE, "Invalid supervisor request");
            logger.error("Invalid supervisor request");

        }
    }

    private void handleAddSupervisor(JSONObject jsonRequest) {
        String supervisorName = jsonRequest.optString("supervisorName");
        if (supervisorName.isEmpty()) {
            logger.error("Invalid request. Unable to find supervisorName");
            handleErrorMessage(INVALID_MESSAGE_TYPE, "Invalid request. Unable to find supervisorName");
            return;
        }

        supervisorService.addSupervisor(supervisorName);
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("status", "success");
        jsonResponse.put("message", "Supervisor added successfully");
        zmqResponseSocket.send(jsonResponse.toString());
    }

    private void handleAttendStudent(JSONObject jsonRequest) {
        String supervisorName = jsonRequest.optString("supervisorName");
        String message = jsonRequest.optString("message");

        if (supervisorName.isEmpty() || message.isEmpty()) {
            logger.warn("Invalid request. Supervisor name or message not found");
            handleErrorMessage(INVALID_MESSAGE_TYPE, "Invalid request. SupervisorName or message is missing");
            return;
        }

        String studentName = supervisorService.assignStudentToSupervisor(supervisorName, message);
        if (studentName.isEmpty()) {
            handleErrorMessage(INVALID_MESSAGE_TYPE, "Failed to attend students");
            return;
        }

        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("message", "Attending: " + studentName);
        jsonResponse.put("status", "success");
        zmqResponseSocket.send(jsonResponse.toString());
    }


    private void handleMakeAvailable(JSONObject jsonRequest) {
        String supervisorName = jsonRequest.optString("supervisorName");
        if (supervisorName.isEmpty()) {
            logger.warn("Invalid request. Could not find supervisorName");
            handleErrorMessage(INVALID_MESSAGE_TYPE, "Invalid request. Could not find supervisorName");
            return;
        }

        supervisorService.makeSupervisorAvailable(supervisorName);
        JSONObject jsonResponse = new JSONObject();
        jsonResponse.put("status", "success");
        jsonResponse.put("message", "Supervisor is now available");
        zmqResponseSocket.send(jsonResponse.toString());
    }


    private void handleStudentRequest(JSONObject json) {
        try {
            String name = json.getString("name");
            String clientId = json.getString("clientId");

            studentService.manageStudent(name, clientId);
            int ticket = studentService.getTicket(name);

            JSONObject responseJson = new JSONObject();
            responseJson.put("ticket", ticket);
            responseJson.put("name", name);
            zmqResponseSocket.send(responseJson.toString());
        } catch (Exception e) {
            logger.error("Error parsing client request.", e);
            handleErrorMessage(INVALID_MESSAGE_TYPE, "invalid queue request");
        }
    }


}