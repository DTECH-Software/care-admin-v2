package com.dtech.admin.mapper;

import com.dtech.admin.dto.response.DependentDetailsResponseDTO;
import com.dtech.admin.mapper.entityToDto.DependentDetailsMapperEntityToDto;
import com.dtech.admin.model.ClaimsDependents;
import com.dtech.admin.service.DocumentStorageService;
import org.junit.jupiter.api.Test;
import org.modelmapper.ModelMapper;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class DependentDetailsMapperEntityToDtoTest {
    @Test
    void mapsCreateAndModificationDates() {
        DependentDetailsMapperEntityToDto mapper = new DependentDetailsMapperEntityToDto(
                new ModelMapper(), mock(DocumentStorageService.class));
        ClaimsDependents dependent = new ClaimsDependents();
        dependent.setId(1L);
        Date createdDate = new Date(1_700_000_000_000L);
        Date modificationDate = new Date(1_710_000_000_000L);
        dependent.setCreatedDate(createdDate);
        dependent.setLastModifiedDate(modificationDate);

        DependentDetailsResponseDTO response = mapper.mapDependentDetails(dependent);

        assertEquals(createdDate, response.getCreatedDate());
        assertEquals(modificationDate, response.getLastModifiedDate());
    }
}
