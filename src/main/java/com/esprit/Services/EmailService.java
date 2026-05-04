package com.esprit.services;

import com.azure.communication.email.EmailClient;
import com.azure.communication.email.EmailClientBuilder;
import com.azure.communication.email.models.EmailMessage;
import com.azure.communication.email.models.EmailSendResult;
import com.azure.core.util.polling.PollResponse;
import com.azure.core.util.polling.SyncPoller;
import io.github.cdimascio.dotenv.Dotenv;
import com.esprit.utils.MyDataBase;
import java.util.List;

public class EmailService {

    private final EmailClient emailClient;
    private final String senderAddress;

    public EmailService() {
        // Chargement des variables d'environnement depuis le fichier .env
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
        
        String endpoint = dotenv.get("AZURE_COMMUNICATION_ENDPOINT");
        String accessKey = dotenv.get("AZURE_COMMUNICATION_ACCESS_KEY");
        this.senderAddress = dotenv.get("AZURE_COMMUNICATION_SENDER");

        if (endpoint == null || accessKey == null || senderAddress == null) {
            System.err.println("ERREUR : Les variables d'environnement Azure Communication Services ne sont pas definies dans le fichier .env.");
        }

        // Initialisation du client Azure
        String connectionString = "endpoint=" + endpoint + ";accesskey=" + accessKey;
        this.emailClient = new EmailClientBuilder()
            .connectionString(connectionString)
            .buildClient();
    }

    /**
     * Envoie un email en utilisant Azure Communication Services.
     * @param to L'adresse email du destinataire.
     * @param subject Le sujet de l'email.
     * @param message Le contenu du message (en texte brut).
     * @return true si l'envoi a reussi, false sinon.
     */
    public boolean sendEmail(String to, String subject, String message) {
        try {
            EmailMessage emailMessage = new EmailMessage()
                .setSenderAddress(this.senderAddress)
                .setToRecipients(to)
                .setSubject(subject)
                .setBodyPlainText(message);

            // Envoi de l'email et attente du resultat
            SyncPoller<EmailSendResult, EmailSendResult> poller = emailClient.beginSend(emailMessage, null);
            PollResponse<EmailSendResult> response = poller.waitForCompletion();

            boolean success = response.getStatus().isComplete();
            
            // Log to database
            MyDataBase.getInstance().insertEmailLog(
                this.senderAddress, 
                to, 
                subject, 
                success ? "sent" : "failed"
            );

            return success;
        } catch (Exception e) {
            e.printStackTrace();
            // Log failure to database
            MyDataBase.getInstance().insertEmailLog(this.senderAddress, to, subject, "failed");
            return false;
        }
    }

    /**
     * Envoie un email a tous les administrateurs.
     * @param subject Le sujet de l'email.
     * @param message Le contenu du message.
     */
    public void sendEmailToAdmins(String subject, String message) {
        List<String> adminEmails = MyDataBase.getInstance().findAllAdminEmails();
        for (String adminEmail : adminEmails) {
            sendEmail(adminEmail, subject, message);
        }
    }
}

