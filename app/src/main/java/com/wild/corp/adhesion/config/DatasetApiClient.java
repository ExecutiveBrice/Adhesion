package com.wild.corp.adhesion.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.wild.corp.adhesion.client.vacances.api.DatasetApi;
import org.wild.corp.adhesion.client.vacances.model.Record;
import org.wild.corp.adhesion.client.vacances.model.Records;

/** HTTP implementation of the OpenDataSoft API generated from vacances.json. */
@Component
public class DatasetApiClient implements DatasetApi {

    private final RestClient educationClient;
    private final RestClient joursFeriesClient;
    private final String joursFeriesDataset;

    public DatasetApiClient(RestClient.Builder builder,
                            @Value("${adhesion.calendrier.education-url:https://data.education.gouv.fr/api/explore/v2.1}") String educationUrl,
                            @Value("${adhesion.calendrier.jours-feries-url:https://public.opendatasoft.com/api/explore/v2.1}") String joursFeriesUrl,
                            @Value("${adhesion.calendrier.jours-feries-dataset:jours-feries-en-france}") String joursFeriesDataset) {
        this.educationClient = builder.clone().baseUrl(educationUrl).build();
        this.joursFeriesClient = builder.clone().baseUrl(joursFeriesUrl).build();
        this.joursFeriesDataset = joursFeriesDataset;
    }

    @Override
    public ResponseEntity<Record> getRecord(String datasetId, String recordId, String select, String lang,
                                             String timezone) {
        Record body = client(datasetId).get().uri(uri -> uri
                        .path("/catalog/datasets/{datasetId}/records/{recordId}")
                        .queryParamIfPresent("select", java.util.Optional.ofNullable(select))
                        .queryParamIfPresent("lang", java.util.Optional.ofNullable(lang))
                        .queryParamIfPresent("timezone", java.util.Optional.ofNullable(timezone))
                        .build(datasetId, recordId))
                .retrieve().body(Record.class);
        return ResponseEntity.ok(body);
    }

    @Override
    public ResponseEntity<Records> getRecords(String datasetId, String select, String where, String groupBy,
                                               String orderBy, Integer limit, Integer offset, String refine,
                                               String exclude, String lang, String timezone, Boolean includeLinks,
                                               Boolean includeAppMetas) {
        Records body = client(datasetId).get().uri(uri -> uri
                        .path("/catalog/datasets/{datasetId}/records")
                        .queryParamIfPresent("select", java.util.Optional.ofNullable(select))
                        .queryParamIfPresent("where", java.util.Optional.ofNullable(where))
                        .queryParamIfPresent("group_by", java.util.Optional.ofNullable(groupBy))
                        .queryParamIfPresent("order_by", java.util.Optional.ofNullable(orderBy))
                        .queryParamIfPresent("limit", java.util.Optional.ofNullable(limit))
                        .queryParamIfPresent("offset", java.util.Optional.ofNullable(offset))
                        .queryParamIfPresent("refine", java.util.Optional.ofNullable(refine))
                        .queryParamIfPresent("exclude", java.util.Optional.ofNullable(exclude))
                        .queryParamIfPresent("lang", java.util.Optional.ofNullable(lang))
                        .queryParamIfPresent("timezone", java.util.Optional.ofNullable(timezone))
                        .queryParamIfPresent("include_links", java.util.Optional.ofNullable(includeLinks))
                        .queryParamIfPresent("include_app_metas", java.util.Optional.ofNullable(includeAppMetas))
                        .build(datasetId))
                .retrieve().body(Records.class);
        return ResponseEntity.ok(body);
    }

    private RestClient client(String datasetId) {
        return joursFeriesDataset.equals(datasetId) ? joursFeriesClient : educationClient;
    }
}
