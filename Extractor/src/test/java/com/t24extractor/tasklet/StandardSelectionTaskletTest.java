package com.t24extractor.tasklet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.t24extractor.model.TafjVoc;
import com.t24extractor.repository.t24_repo.TafjVocRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StandardSelectionTaskletTest {

  private static final Logger log = LoggerFactory.getLogger(StandardSelectionTaskletTest.class);
  @Mock
  private TafjVocRepository tafjVocRepository;
  @InjectMocks
  private StandardSelectionTasklet standardSelectionTasklet;

  @BeforeEach
  void init() {
    tafjVocRepository = mock(TafjVocRepository.class);
    standardSelectionTasklet = new StandardSelectionTasklet();
  }

  @Test
  void should_return_is_blob_from_tafj_voc() {
    // Given

    TafjVoc tafjVoc = new TafjVoc("CUSTOMER", "X", "FBNK_CUSTOMER");

    // When calling the mocked repository
    when(tafjVocRepository.findTafjVocByOrclFileName(tafjVoc.orclFileName())).thenReturn(
        Optional.of(tafjVoc));

    assertEquals("X", tafjVoc.isBlob());

  }
}
