package com.wild.corp.adhesion.models.newsletter;

public record NewsletterCampaign(long id, String name, String subject, String previewText,
                                 String htmlContent, long listId, String listName, Long subscribers,
                                 String brevoStatus, String confirmationVersion) { }
