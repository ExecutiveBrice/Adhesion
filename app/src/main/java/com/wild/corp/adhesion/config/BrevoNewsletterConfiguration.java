package com.wild.corp.adhesion.config;

import brevo.ApiClient;
import brevo.auth.ApiKeyAuth;
import brevoApi.ContactsApi;
import brevoApi.EmailCampaignsApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BrevoNewsletterConfiguration {
    @Bean
    public EmailCampaignsApi newsletterCampaignsApi(@Value("${brevo.newsletter.api-key:}") String apiKey) {
        ApiClient client = newsletterContactsApi(apiKey).getApiClient();
        // The published API uses inProcess; this SDK also uses in_process.
        client.getJson().setGson(client.getJson().getGson().newBuilder()
                .registerTypeAdapter(brevoModel.GetEmailCampaign.StatusEnum.class,
                        new com.google.gson.TypeAdapter<brevoModel.GetEmailCampaign.StatusEnum>() {
                            @Override
                            public void write(com.google.gson.stream.JsonWriter out, brevoModel.GetEmailCampaign.StatusEnum value)
                                    throws java.io.IOException {
                                if (value == null) out.nullValue(); else out.value(value.getValue());
                            }
                            @Override
                            public brevoModel.GetEmailCampaign.StatusEnum read(com.google.gson.stream.JsonReader in)
                                    throws java.io.IOException {
                                if (in.peek() == com.google.gson.stream.JsonToken.NULL) { in.nextNull(); return null; }
                                String value = in.nextString();
                                if ("inProcess".equals(value)) value = "in_process";
                                if ("inReview".equals(value)) value = "in_review";
                                return brevoModel.GetEmailCampaign.StatusEnum.fromValue(value);
                            }
                        }).create());
        return new EmailCampaignsApi(client);
    }

    @Bean
    public ContactsApi newsletterContactsApi(@Value("${brevo.newsletter.api-key:}") String apiKey) {
        // A private client: never change the global client used by EmailService.
        ApiClient client = new ApiClient();
        ((ApiKeyAuth) client.getAuthentication("api-key")).setApiKey(apiKey);
        client.setConnectTimeout(5_000).setReadTimeout(10_000).setWriteTimeout(10_000);
        client.setHttpClient(client.getHttpClient().newBuilder().retryOnConnectionFailure(false)
                .followRedirects(false).followSslRedirects(false).build());
        return new ContactsApi(client);
    }
}
