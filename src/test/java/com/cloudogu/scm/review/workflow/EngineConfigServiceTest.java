package com.cloudogu.scm.review.workflow;

import org.apache.shiro.authz.UnauthorizedException;
import org.github.sdorra.jse.ShiroExtension;
import org.github.sdorra.jse.SubjectAware;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import sonia.scm.repository.Repository;
import sonia.scm.repository.RepositoryTestData;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, ShiroExtension.class})
@SubjectAware(value = "Trillian")
class EngineConfigServiceTest {

  private final Repository repository = RepositoryTestData.create42Puzzle();

  @Mock
  private RepositoryEngineConfigurator repositoryEngineConfigurator;
  @Mock
  private GlobalEngineConfigurator globalEngineConfigurator;

  @InjectMocks
  private EngineConfigService engineConfigService;

  @Test
  void shouldThrowExceptionIfNotPermittedToReadConfig() {
    assertThrows(UnauthorizedException.class, () -> engineConfigService.getRepositoryEngineConfig(repository));
  }

  @Test
  void shouldThrowExceptionIfNotPermittedToWriteConfig() {
    assertThrows(UnauthorizedException.class, () -> engineConfigService.setRepositoryEngineConfig(repository, new EngineConfiguration()));
  }

  @Test
  void shouldThrowExceptionIfNotPermittedToReadGlobalConfig() {
    assertThrows(UnauthorizedException.class, () -> engineConfigService.getGlobalEngineConfig());
  }

  @Test
  void shouldThrowExceptionIfNotPermittedToWriteGlobalConfig() {
    assertThrows(UnauthorizedException.class, () -> engineConfigService.setGlobalEngineConfig(new GlobalEngineConfiguration()));
  }


  @Test
  void shouldReturnTrueIfRepoEngineIsEnabledAndAllowedGlobally() {
    EngineConfiguration engineConfiguration = new EngineConfiguration();
    engineConfiguration.setEnabled(true);
    when(repositoryEngineConfigurator.getEngineConfiguration(repository)).thenReturn(engineConfiguration);
    when(globalEngineConfigurator.getEngineConfiguration()).thenReturn(new GlobalEngineConfiguration());

    boolean active = engineConfigService.isEngineActiveForRepository(repository);

    assertThat(active).isTrue();
  }

  @Test
  void shouldReturnFalseIfRepoEngineIsEnabledButNotAllowedGlobally() {
    EngineConfiguration engineConfiguration = new EngineConfiguration();
    engineConfiguration.setEnabled(true);
    when(repositoryEngineConfigurator.getEngineConfiguration(repository)).thenReturn(engineConfiguration);
    GlobalEngineConfiguration globalEngineConfiguration = new GlobalEngineConfiguration();
    globalEngineConfiguration.setDisableRepositoryConfiguration(true);
    when(globalEngineConfigurator.getEngineConfiguration()).thenReturn(globalEngineConfiguration);

    boolean active = engineConfigService.isEngineActiveForRepository(repository);

    assertThat(active).isFalse();
  }

  @Test
  void shouldReturnTrueIfRepoEngineIsDisabledButGlobalEngineIsEnabled() {
    EngineConfiguration engineConfiguration = new EngineConfiguration();
    engineConfiguration.setEnabled(false);
    when(repositoryEngineConfigurator.getEngineConfiguration(repository)).thenReturn(engineConfiguration);
    GlobalEngineConfiguration globalEngineConfiguration = new GlobalEngineConfiguration();
    globalEngineConfiguration.setEnabled(true);
    when(globalEngineConfigurator.getEngineConfiguration()).thenReturn(globalEngineConfiguration);

    boolean active = engineConfigService.isEngineActiveForRepository(repository);

    assertThat(active).isTrue();
  }

  @Test
  void shouldReturnFalseIfBothConfigsAreDisabled() {
    EngineConfiguration engineConfiguration = new EngineConfiguration();
    engineConfiguration.setEnabled(false);
    when(repositoryEngineConfigurator.getEngineConfiguration(repository)).thenReturn(engineConfiguration);
    GlobalEngineConfiguration globalEngineConfiguration = new GlobalEngineConfiguration();
    globalEngineConfiguration.setEnabled(false);
    when(globalEngineConfigurator.getEngineConfiguration()).thenReturn(globalEngineConfiguration);

    boolean active = engineConfigService.isEngineActiveForRepository(repository);

    assertThat(active).isFalse();
  }

  @Test
  void shouldReturnEffectiveRepoConfig() {
    EngineConfiguration engineConfiguration = new EngineConfiguration();
    engineConfiguration.setEnabled(true);
    GlobalEngineConfiguration globalEngineConfiguration = new GlobalEngineConfiguration();
    when(globalEngineConfigurator.getEngineConfiguration()).thenReturn(globalEngineConfiguration);
    when(repositoryEngineConfigurator.getEngineConfiguration(repository)).thenReturn(engineConfiguration);

    EngineConfiguration effectiveEngineConfig = engineConfigService.getEffectiveEngineConfig(repository);

    assertThat(effectiveEngineConfig).isEqualTo(engineConfiguration);
  }

  @Test
  void shouldReturnEffectiveGlobalConfig() {
    EngineConfiguration engineConfiguration = new EngineConfiguration();
    GlobalEngineConfiguration globalEngineConfiguration = new GlobalEngineConfiguration();
    globalEngineConfiguration.setEnabled(true);
    globalEngineConfiguration.setDisableRepositoryConfiguration(true);
    when(globalEngineConfigurator.getEngineConfiguration()).thenReturn(globalEngineConfiguration);
    when(repositoryEngineConfigurator.getEngineConfiguration(repository)).thenReturn(engineConfiguration);

    EngineConfiguration effectiveEngineConfig = engineConfigService.getEffectiveEngineConfig(repository);

    assertThat(effectiveEngineConfig).isEqualTo(globalEngineConfiguration);
  }


  @Nested
  class WithPermissions {

    private final EngineConfiguration engineConfiguration = new EngineConfiguration();
    private final GlobalEngineConfiguration globalEngineConfiguration = new GlobalEngineConfiguration();

    @BeforeEach
    void mockConfigurators() {
      repository.setId("id-1");
      lenient().when(globalEngineConfigurator.getEngineConfiguration()).thenReturn(globalEngineConfiguration);
      lenient().when(repositoryEngineConfigurator.getEngineConfiguration(repository)).thenReturn(engineConfiguration);
    }

    @Test
    @SubjectAware(permissions = "repository:readWorkflowConfig:id-1")
    void shouldReturnRepoConfig() {
      EngineConfiguration repositoryEngineConfig = engineConfigService.getRepositoryEngineConfig(repository);

      assertThat(repositoryEngineConfig).isEqualTo(engineConfiguration);
    }

    @Test
    @SubjectAware(permissions = "configuration:read:workflowConfig")
    void shouldReturnGlobalConfig() {
      GlobalEngineConfiguration globalEngineConfig = engineConfigService.getGlobalEngineConfig();

      assertThat(globalEngineConfig).isEqualTo(globalEngineConfiguration);
    }

    @Test
    @SubjectAware(permissions = "repository:writeWorkflowConfig:id-1")
    void shouldSetRepoConfig() {
      engineConfigService.setRepositoryEngineConfig(repository, engineConfiguration);

      verify(repositoryEngineConfigurator).setEngineConfiguration(repository, engineConfiguration);
    }

    @Test
    @SubjectAware(permissions = "configuration:write:workflowConfig")
    void shouldSetGlobalConfig() {
      engineConfigService.setGlobalEngineConfig(globalEngineConfiguration);

      verify(globalEngineConfigurator).setEngineConfiguration(globalEngineConfiguration);
    }
  }
}
