package org.jenkinsci.plugins.globalshellhook;

import hudson.Extension;
import hudson.model.Describable;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.model.listeners.RunListener;
import hudson.model.AbstractProject;
import jenkins.model.Jenkins;

import net.sf.json.JSONObject;

import org.kohsuke.stapler.StaplerRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Map;

@Extension
public class GlobalShellHook extends RunListener<Run<?, ?>>
    implements Describable<GlobalShellHook> {

  public GlobalShellHook() {
  }

  @Override
  public void onCompleted(Run run, TaskListener listener) {
    String shellScript = getDescriptorImpl().getShellScript();
    if (shellScript.equals("")) {
      return;
    }

    PrintStream logger = listener.getLogger();

    ProcessBuilder pb = new ProcessBuilder("/bin/sh", "-c", shellScript);
    pb.redirectErrorStream();
    Map<String, String> env = pb.environment();
    env.put("RESULT", run.getResult().toString());
    env.put("JOB_NAME", run.getParent().getName());

    try {
      Process process = pb.start();
      process.waitFor();
      InputStream inputStream = process.getInputStream();
      logger.println(streamToString(inputStream));
    } catch (IOException e) {
      logger.print("error occured with the shell script:" + e.toString());
    } catch (InterruptedException e) {
      logger.print("error occured with the shell script:" + e.toString());
    }
  }

  String streamToString(InputStream in) throws IOException {
    StringBuilder out = new StringBuilder();
    BufferedReader br = new BufferedReader(new InputStreamReader(in));
    for (String line = br.readLine(); line != null; line = br.readLine()) {
      out.append(line);
    }
    br.close();
    return out.toString();
  }


  @Override
  public Descriptor<GlobalShellHook> getDescriptor() {
    return getDescriptor();
  }

  public DescriptorImpl getDescriptorImpl() {
    return (DescriptorImpl) Jenkins.getInstance().getDescriptorOrDie(GlobalShellHook.class);
  }


  /**
   * Descriptor for {@link GlobalShellHook}. Used as a singleton. The class is marked as public so
   * that it can be accessed from views.
   *
   * <p> See <tt>src/main/resources/hudson/plugins/hello_world/GlobalShellHook/*.jelly</tt> for
   * the actual HTML fragment for the configuration screen.
   */
  @Extension // This indicates to Jenkins that this is an implementation of an extension point.
  public static final class DescriptorImpl extends Descriptor<GlobalShellHook> {

    /**
     * To persist global configuration information, simply store it in a field and call save().
     *
     * <p> If you don't want fields to be persisted, use <tt>transient</tt>.
     */
    private String shellScript = "";

    /**
     * In order to load the persisted global configuration, you have to call load() in the
     * constructor.
     */
    public DescriptorImpl() {
      load();
    }

    public boolean isApplicable(Class<? extends AbstractProject> aClass) {
      // Indicates that this builder can be used with all kinds of project types
      return true;
    }

    /**
     * This human readable name is used in the configuration screen.
     */
    public String getDisplayName() {
      return "Global Shell Hook";
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
      // To persist global configuration information,
      // set that to properties and call save().

      shellScript = formData.getString("shellScript");

      // ^Can also use req.bindJSON(this, formData);
      //  (easier when there are many fields; need set* methods for this, like setUseFrench)
      save();
      return super.configure(req, formData);
    }

    /**
     * This method returns true if the global configuration says we should speak French.
     *
     * The method name is bit awkward because global.jelly calls this method to determine the
     * initial state of the checkbox by the naming convention.
     */
    public String getShellScript() {
      return this.shellScript;
    }
  }
}

