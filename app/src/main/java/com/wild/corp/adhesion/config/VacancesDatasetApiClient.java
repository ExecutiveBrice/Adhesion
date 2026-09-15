package com.wild.corp.adhesion.config;

import org.springframework.cloud.openfeign.FeignClient;
import org.wild.corp.adhesion.client.vacances.api.DatasetApi;

/** Client Feign de l'API OpenDataSoft du ministère de l'Éducation nationale. */
@FeignClient(
        name = "vacancesDatasetApi",
        contextId = "vacancesDatasetApi",
        qualifiers = "vacancesDatasetApi",
        url = "${adhesion.calendrier.education-url:https://data.education.gouv.fr/api/explore/v2.1}"
)
public interface VacancesDatasetApiClient extends DatasetApi {
}
