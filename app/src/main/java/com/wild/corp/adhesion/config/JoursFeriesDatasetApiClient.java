package com.wild.corp.adhesion.config;

import org.springframework.cloud.openfeign.FeignClient;
import org.wild.corp.adhesion.client.vacances.api.DatasetApi;

/** Client Feign du jeu de données OpenDataSoft des jours fériés français. */
@FeignClient(
        name = "joursFeriesDatasetApi",
        contextId = "joursFeriesDatasetApi",
        qualifiers = "joursFeriesDatasetApi",
        url = "${adhesion.calendrier.jours-feries-url:https://public.opendatasoft.com/api/explore/v2.1}"
)
public interface JoursFeriesDatasetApiClient extends DatasetApi {
}
