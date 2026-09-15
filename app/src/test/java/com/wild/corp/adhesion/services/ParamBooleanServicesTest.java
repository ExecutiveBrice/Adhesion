package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.ParamBoolean;
import com.wild.corp.adhesion.repository.ParamBooleanRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ParamBooleanServicesTest {

    @Test
    void createsMailSendingParameterEnabledByDefault() {
        ParamBooleanRepository repository = mock(ParamBooleanRepository.class);
        when(repository.existsByParamName(anyString())).thenReturn(true);
        when(repository.existsByParamName("Envoi_Mails")).thenReturn(false);

        ParamBooleanServices service = new ParamBooleanServices();
        service.paramBooleanRepository = repository;

        service.fillParamBoolean();

        ArgumentCaptor<ParamBoolean> parameter = ArgumentCaptor.forClass(ParamBoolean.class);
        verify(repository).save(parameter.capture());
        assertThat(parameter.getValue())
                .extracting(ParamBoolean::getParamName, ParamBoolean::getParamValue)
                .containsExactly("Envoi_Mails", true);
    }
}
