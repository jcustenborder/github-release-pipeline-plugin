package com.github.jcustenborder.jenkins.github.release;

import com.google.common.base.Charsets;
import com.google.common.io.Files;
import hudson.Extension;
import hudson.util.DirScanner;
import hudson.util.FileVisitor;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.workflow.steps.AbstractStepDescriptorImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractStepImpl;
import org.jenkinsci.plugins.workflow.steps.AbstractSynchronousNonBlockingStepExecution;
import org.kohsuke.github.GHRelease;
import org.kohsuke.github.GHReleaseBuilder;
import org.kohsuke.github.GHRepository;
import org.kohsuke.github.GitHub;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class GitHubReleaseStep extends AbstractStepImpl {

  @DataBoundConstructor
  public GitHubReleaseStep() {

  }

  private transient String token;
  private transient String apiUrl;
  private transient String repositoryName;
  private transient String tagName;
  private transient boolean preRelease;
  private transient boolean draft;
  private String commitish;
  private String descriptionFile;
  private String description;
  private String includes;
  private String excludes;


  /**
   * Optional apiUrl of the github server.
   *
   * @param apiUrl optional apiUrl of the github server.
   */
  @DataBoundSetter
  public void setApiUrl(String apiUrl) {
    this.apiUrl = apiUrl;
  }

  public String getApiUrl() {
    return apiUrl;
  }

  /**
   * Optional commit to create the release from.
   *
   * @param commitish optional commit to create the release from.
   */
  @DataBoundSetter
  public void setCommitish(String commitish) {
    this.commitish = commitish;
  }

  public String getCommitish() {
    return commitish;
  }

  /**
   * Description to use for the body of the release.
   *
   * @param description optional Description to use for the body of the release.
   */
  @DataBoundSetter
  public void setDescription(String description) {
    this.description = description;
  }

  /**
   * Description file to use for the body of the release.
   *
   * @param descriptionFile optional Description file to use for the body of the release.
   */
  @DataBoundSetter
  public void setDescriptionFile(String descriptionFile) {
    this.descriptionFile = descriptionFile;
  }

  /**
   * Determines if this release is a draft release.
   *
   * @param draft optional Determines if this release is a draft release.
   */
  @DataBoundSetter
  public void setDraft(boolean draft) {
    this.draft = draft;
  }

  /**
   * Determines if this release is a draft release.
   *
   * @param preRelease optional Determines if this release is a pre release.
   */
  @DataBoundSetter
  public void setPreRelease(boolean preRelease) {
    this.preRelease = preRelease;
  }

  /**
   * Name of the repository on the github server
   *
   * @param repositoryName required Name of the repository on the github server
   */
  @DataBoundSetter
  public void setRepositoryName(String repositoryName) {
    this.repositoryName = repositoryName;
  }

  /**
   * The api token to connect to github with.
   *
   * @param token required The api token to connect to github with.
   */
  @DataBoundSetter
  public void setToken(String token) {
    this.token = token;
  }

  /**
   * The name of the tag to create.
   *
   * @param tagName required The name of the tag to create.
   */
  @DataBoundSetter
  public void setTagName(String tagName) {
    this.tagName = tagName;
  }

  @Extension
  public static class DescriptorImpl extends AbstractStepDescriptorImpl {

    public DescriptorImpl() {
      super(Execution.class);
    }

    @Override
    public String getFunctionName() {
      return "githubRelease";
    }

    @Override
    public String getDisplayName() {
      return "Read a maven project file.";
    }

  }

  public static class Execution extends AbstractSynchronousNonBlockingStepExecution<String> {
    private static final long serialVersionUID = 1L;

    @Inject
    private transient GitHubReleaseStep step;

    @Override
    protected String run() throws Exception {
      if (StringUtils.isBlank(this.step.token)) {
        throw new IllegalStateException("token cannot be blank.");
      }

      if (StringUtils.isBlank(this.step.repositoryName)) {
        throw new IllegalStateException("repositoryName cannot be blank.");
      }

      if (StringUtils.isBlank(this.step.tagName)) {
        throw new IllegalStateException("tagName cannot be blank.");
      }

      if (StringUtils.isBlank(this.step.descriptionFile) && StringUtils.isBlank(this.step.description)) {
        throw new IllegalStateException("Either descriptionFile or description must be specified.");
      }

      if (!StringUtils.isBlank(this.step.descriptionFile)) {
        File inputFile = new File(this.step.descriptionFile);
        this.step.description = Files.toString(inputFile, Charsets.UTF_8);
      }

      GitHub github;

      if (StringUtils.isBlank(this.step.apiUrl)) {
        github = GitHub.connectUsingOAuth(this.step.token);
      } else {
        github = GitHub.connectToEnterprise(this.step.apiUrl, this.step.token);
      }

      GHRepository repository = github.getRepository(this.step.repositoryName);
      GHReleaseBuilder releaseBuilder = repository.createRelease(this.step.tagName)
          .prerelease(this.step.preRelease)
          .draft(this.step.draft);

      if (!StringUtils.isBlank(this.step.commitish)) {
        releaseBuilder.commitish(this.step.commitish);
      }

      final GHRelease release = releaseBuilder.create();

      DirScanner scanner = new DirScanner.Glob(this.step.includes, this.step.excludes, true);

      getContext().get()

      scanner.scan(File., new FileVisitor() {
        @Override
        public void visit(File file, String s) throws IOException {
          Path path = file.toPath();
          String contentType;

          try {
            contentType = java.nio.file.Files.probeContentType(path);
          } catch (IOException ex) {
            contentType = "application/octet-stream";
          }

          release.uploadAsset(file, contentType);
        }
      });

      return release.getHtmlUrl().toString();
    }
  }
}
