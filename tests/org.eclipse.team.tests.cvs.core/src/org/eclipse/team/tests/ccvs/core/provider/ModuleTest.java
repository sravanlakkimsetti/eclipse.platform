package org.eclipse.team.tests.ccvs.core.provider;
/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.internal.plugins.PluginDescriptor;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.team.ccvs.core.CVSTag;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.sync.IRemoteSyncElement;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.client.Session;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.core.resources.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.resources.ICVSResource;
import org.eclipse.team.internal.ccvs.core.resources.RemoteModule;
import org.eclipse.team.tests.ccvs.core.CVSTestSetup;
import org.eclipse.team.tests.ccvs.core.EclipseTest;

/**
 * This class tests the Command framework using simple CVS commands
 */
public class ModuleTest extends EclipseTest {
	
	public static final String RESOURCE_PATH = "resources/CommandTest/";
	
	public ModuleTest() {
		super(null);
	}
	
	public ModuleTest(String name) {
		super(name);
	}
	
	public static Test suite() {
		TestSuite suite = new TestSuite(ModuleTest.class);
		//return new CVSTestSetup(suite);
		return new CVSTestSetup(new ModuleTest("testSelfReferencingModule"));
	}
	
	private static boolean isSetUp = false;
	
	private static RemoteModule[] remoteModules;
	
	public void setUp() throws TeamException, CoreException, IOException {
		if (isSetUp) return;
		
		// upload the modules definitions file
		PluginDescriptor testPlugin = (PluginDescriptor)Platform.getPluginRegistry().getPluginDescriptor("org.eclipse.team.tests.cvs.core");
		String filePath = testPlugin.getLocation().concat(RESOURCE_PATH + "CVSROOT/modules");
		URL url = null;
		try {
			url = new URL (filePath);
		} catch (java.net.MalformedURLException e) {
			assertTrue("Bad URL for " + filePath, true);
		}

		waitMsec(1000);

		IProject cvsroot = checkoutProject(null, "CVSROOT", null);
		InputStream in = url.openStream();
		try {
			cvsroot.getFile("modules").setContents(in, false, false, DEFAULT_MONITOR);
		} finally {
			in.close();
		}
		commitProject(cvsroot);
		
		uploadProject("common");
		
		remoteModules = getRemoteModules();
		
		isSetUp = true;
	}
	
	protected void uploadProject(String projectName) throws TeamException, IOException {
		// upload the modules definitions file
		PluginDescriptor testPlugin = (PluginDescriptor)Platform.getPluginRegistry().getPluginDescriptor("org.eclipse.team.tests.cvs.core");
		String filePath = testPlugin.getLocation().concat(RESOURCE_PATH + projectName);
		URL url = null;
		try {
			url = new URL (filePath);
		} catch (java.net.MalformedURLException e) {
			assertTrue("Bad URL for " + filePath, true);
		}
		
		// Import the project into CVS
		Session s = new Session(getRepository(), Session.getManagedFolder(new File(url.getPath())));
		s.open(DEFAULT_MONITOR);
		try {
			Command.IMPORT.execute(s, Command.NO_GLOBAL_OPTIONS, 
				new LocalOption[] {Command.makeMessageOption("")},
				new String[] { projectName, "start", "vendor"},
				null,
				DEFAULT_MONITOR);
		} finally {
			s.close();
		}
	}
	
	// Commit the project on checkout to ensure that the .vcm_meta is created
	// XXX Temporary measure!!!
	protected IProject checkoutProject(IProject project, String moduleName, CVSTag tag) throws TeamException {
		project = super.checkoutProject(project, moduleName, tag);
		getProvider(project).add(new IResource[] {project.getFile(".vcm_meta")}, IResource.DEPTH_INFINITE, DEFAULT_MONITOR);
		waitMsec(1000);
		commitProject(project);
		return project;
	}
	
	/**
	 * wait milliseconds to continou the execution
	 */
	protected static void waitMsec(int msec) {	
		try {
			Thread.currentThread().sleep(msec);
		} catch(InterruptedException e) {
			fail("wait-problem");
		}
	}

	/*
	 * Test the following definition
	 * 
	 *   # self referencing modules
	 *   project1 project1
	 */
	public void testSelfReferencingModule() throws TeamException, CoreException, IOException {
		uploadProject("project1");
		IProject project1 = checkoutProject(null, "project1", null);
		IRemoteSyncElement tree = getProvider(project1).getRemoteSyncTree(project1, CVSTag.DEFAULT, DEFAULT_MONITOR);
		assertEquals("Local does not match remote", Session.getManagedResource(project1), (ICVSResource)tree.getRemote(), false, false);
		RemoteModule module = getRemoteModule("project1");
		assertEquals("Local does not match module", Session.getManagedResource(project1), module, false, false);
	}
	
	/*
	 * Test the following definition
	 * 
	 * # checkout docs in flattened structure
	 * docs		-d docs common/docs
	 * macros common/macros
	 */
	public void testFlattenedStructure() throws TeamException, CoreException, IOException {
		
		IProject docs = checkoutProject(null, "docs", null);
		IRemoteSyncElement tree = getProvider(docs).getRemoteSyncTree(docs, CVSTag.DEFAULT, DEFAULT_MONITOR);
		assertEquals("Local does not match remote", Session.getManagedResource(docs), (ICVSResource)tree.getRemote(), false, false);
		RemoteModule module = getRemoteModule("docs");
		assertEquals("Local does not match module", Session.getManagedResource(docs), module, false, false);

		
		IProject macros = checkoutProject(null, "macros", null);
		tree = getProvider(macros).getRemoteSyncTree(macros, CVSTag.DEFAULT, DEFAULT_MONITOR);
		assertEquals("Local does not match remote", Session.getManagedResource(macros), (ICVSResource)tree.getRemote(), false, false);
		module = getRemoteModule("macros");
		assertEquals("Local does not match module", Session.getManagedResource(macros), module, false, false);

	}
	
	/*
	 * Test the following definition
	 * 
	 * # include docs with project
	 * project2		project2 &docs
	 * # only project2
	 * project2-only project2
	 */
	public void testIncludeAndExcludeDocs() throws TeamException, CoreException, IOException {
		uploadProject("project2");
		IProject project2 = checkoutProject(null, "project2", null);
		IRemoteSyncElement tree = getProvider(project2).getRemoteSyncTree(project2, CVSTag.DEFAULT, DEFAULT_MONITOR);
		assertEquals("Local does not match remote", Session.getManagedResource(project2), (ICVSResource)tree.getRemote(), false, false);

		RemoteModule module = getRemoteModule("project2");
		assertEquals("Local does not match module", Session.getManagedResource(project2), module, false, false);

		project2 = checkoutProject(null, "project2-only", null);
		tree = getProvider(project2).getRemoteSyncTree(project2, CVSTag.DEFAULT, DEFAULT_MONITOR);
		assertEquals("Local does not match remote", Session.getManagedResource(project2), (ICVSResource)tree.getRemote(), false, false);

		module = getRemoteModule("project2-only");
		assertEquals("Local does not match module", Session.getManagedResource(project2), module, false, false);

	}
	
	/*
	 * Test the following definition
	 * 
	 * # a use of alias
	 * project3-src  project3/src
	 * project3-src_file -a project3-src/file.c mc-src/file.h
	 * project3-sub  project3/sub &project3-src_file
	 */
	public void testAliasForFiles() throws TeamException, CoreException, IOException {
		uploadProject("project3");
		IProject project3 = checkoutProject(null, "project3-sub", null);
		IRemoteSyncElement tree = getProvider(project3).getRemoteSyncTree(project3, CVSTag.DEFAULT, DEFAULT_MONITOR);
//		assertEquals("Local does not match remote", Session.getManagedResource(project3), (ICVSResource)tree.getRemote(), false, false);

		project3 = checkoutProject(null, "project3-src", null);
		tree = getProvider(project3).getRemoteSyncTree(project3, CVSTag.DEFAULT, DEFAULT_MONITOR);
//		assertEquals("Local does not match remote", Session.getManagedResource(project3), (ICVSResource)tree.getRemote(), false, false);

		project3 = checkoutProject(null, "project3-src_file", null);
		tree = getProvider(project3).getRemoteSyncTree(project3, CVSTag.DEFAULT, DEFAULT_MONITOR);
//		assertEquals("Local does not match remote", Session.getManagedResource(project3), (ICVSResource)tree.getRemote(), false, false);
	}
	
	/*
	 * Test the following definition
	 * 
	 * # using aliases to provide packaging
	 * project7-common -a project7/common
	 * project7-pc -a project7-common project7/pc
	 * project7-linux -a project7-common project7/linux
	 */
	public void testAliases() throws TeamException, CoreException, IOException {
		uploadProject("project7");
		IProject project7 = checkoutProject(null, "project7-common", null);
		IRemoteSyncElement tree = getProvider(project7).getRemoteSyncTree(project7, CVSTag.DEFAULT, DEFAULT_MONITOR);
//		assertEquals("Local does not match remote", Session.getManagedResource(project7), (ICVSResource)tree.getRemote(), false, false);

		project7 = checkoutProject(null, "project7-pc", null);
		tree = getProvider(project7).getRemoteSyncTree(project7, CVSTag.DEFAULT, DEFAULT_MONITOR);
//		assertEquals("Local does not match remote", Session.getManagedResource(project7), (ICVSResource)tree.getRemote(), false, false);

		project7 = checkoutProject(null, "project7-linux", null);
		tree = getProvider(project7).getRemoteSyncTree(project7, CVSTag.DEFAULT, DEFAULT_MONITOR);
//		assertEquals("Local does not match remote", Session.getManagedResource(project7), (ICVSResource)tree.getRemote(), false, false);
	}
	
	public RemoteModule[] getRemoteModules() throws TeamException {
		
		RemoteModule[] modules;
		Session s = new Session(getRepository(), (ICVSFolder)Session.getManagedResource(ResourcesPlugin.getWorkspace().getRoot()));
		s.open(DEFAULT_MONITOR);
		try {
			modules = Command.CHECKOUT.getRemoteModules(s, null, DEFAULT_MONITOR);
		} finally {
			s.close();
		}
		
		return modules;
	}
	
	public RemoteModule getRemoteModule(String moduleName) {
		for (int i = 0; i < remoteModules.length; i++) {
			RemoteModule module = remoteModules[i];
			// XXX shouldn't be getName
			if (module.getName().equals(moduleName))
				return module;
		}
		return null;
	}
}

