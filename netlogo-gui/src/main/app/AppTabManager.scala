// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.app
import java.awt.Component
import java.awt.event.{ ActionEvent, KeyEvent }
import javax.swing.{ Action, AbstractAction, ActionMap, InputMap, JComponent, JTabbedPane }

import org.nlogo.core.I18N
import org.nlogo.app.codetab.{ CodeTab } // aab MainCodeTab
import org.nlogo.swing.{ UserAction }

// The class AppTabManager handles relationships between tabs (JPanels) and the two
// classes Tabs and MainCodeTabPanel that are the JTabbedPanes that contain them.

class AppTabManager( val appTabsPanel:          Tabs,
                     var codeTabsPanelOption: Option[MainCodeTabPanel]) {

  def getAppTabPanel = appTabsPanel

  def setCodeTabPanel(_codeTabsPanelOption: Option[MainCodeTabPanel]): Unit = {
    codeTabsPanelOption = _codeTabsPanelOption
  }

  def getMainCodeTabOwner =
    codeTabsPanelOption match {
      case None           => appTabsPanel
      case Some(theValue) => theValue
    }

  def isCodeTabSeparate =
    !getMainCodeTabOwner.isInstanceOf[Tabs]

  def isCodeTabAttached =
    getMainCodeTabOwner.isInstanceOf[Tabs]

  def getAppsTab = appTabsPanel
  def getCodeTab = appTabsPanel.getCodeTab
  private var currentTab: Component = appTabsPanel.interfaceTab

  def getTabOwner(tab: Component): AbstractTabs = {
    if (tab.isInstanceOf[CodeTab]) getMainCodeTabOwner else appTabsPanel
  }

  def setSelectedCodeTab(tab: CodeTab): Unit = {
    getMainCodeTabOwner.setSelectedComponent(tab)
  }

  def setSelectedAppTab(index: Int): Unit = {
    appTabsPanel.setSelectedIndex(index)
  }

  def getSelectedAppTabComponent() = appTabsPanel.getSelectedComponent

  def getSelectedAppTabIndex() = appTabsPanel.getSelectedIndex

  def setCurrentTab(tab: Component): Unit = {
    currentTab = tab
  }

  def getCurrentTab(): Component = {
    currentTab
  }

  def getTotalTabCount(): Int = {
    val appTabCount = appTabsPanel.getTabCount
    codeTabsPanelOption match {
      case None           => appTabCount
      case Some(thePanel) => appTabCount + thePanel.getTabCount
    }
  }

  // Before the detachable code tab capability was added
  // the integers associated with tabs in the menu and the keyboard shortcuts
  // were directly connected to an index in JTabbedPane (usually an instance of the class Tabs ),
  // The tabs menu refers to tabs as TabTitle Command-Key index + 1
  // For example
  // Interface Ctrl 1
  // Tab Ctrl 2
  // Code Ctrl 3
  // File1.nls Ctrl 4
  // New File 1 Ctrl 5
  // Besides using the menu item, the user can also use the corresponding
  // keyboard shortcut. For example Ctrl 3 to access the code tab.
  // When the code tab detaches, the same number must be converted into
  // an index into the MainCodeTabPanel.
  // The following terminology will be useful.
  // appTabsPanel = application JTabbedPane (Tabs)
  // codeTabsPanel = MainCodeTabPanel when it exists
  // nAppTabPanel = the number of tabs in appTabsPanel at the moment
  // origTabIndx = the index of a tab in appTabsPanel when there is no codeTabsPanel
  // codeTabIndx = the index of a tab in codeTabsPanel (if it exists)
  // When the codetab is not detached
  // origTabIndx is an index in appTabsPanel
  // When the codetab is detached
  // for origTabIndx < nAppTabPanel: origTabIndx is an index in appTabsPanel
  // origTabIndx >= nAppTabPanel: codeTabIndx = origTabIndx - nAppTabPanel is an index in codeTabsPanel

  // Input: origTabIndx - index a tab would have if there were no separate code tab.
  // Returns (tabOwner, tabIndex)
  // tabOwner = the AbstractTabs containing the indexed tab.
  // tabIndex = the index of the tab in tabOwner.
  // This method allows for the possibility that the appTabsPanel has no tabs,
  // although this should not occur in practice
  @throws (classOf[IndexOutOfBoundsException])
  def computeIndexPlus(origTabIndx: Int): (AbstractTabs, Int) = {
    if (origTabIndx < 0) {
      throw new IndexOutOfBoundsException
    }
    var tabOwner = appTabsPanel.asInstanceOf[AbstractTabs]
    val appTabCount = appTabsPanel.getTabCount
    var tabIndex = origTabIndx

    // if the origTabIndx is too large for the appTabsPanel,
    // check if it can refer to the a separate code tab
    if (origTabIndx >= appTabCount) {
      codeTabsPanelOption match {
        case None           => throw new IndexOutOfBoundsException
        case Some(thePanel) => {
          // origTabIndx could be too large for the two Panels combined
          if (origTabIndx >= appTabCount + thePanel.getTabCount) {
            throw new IndexOutOfBoundsException
          }
          tabOwner = getMainCodeTabOwner
          tabIndex =  origTabIndx - appTabCount
        }
      }
    }
    (tabOwner, tabIndex)
  }

  // Input: tab - a tab component
  // Returns (tabOwner, tabIndex)
  // where tabOwner is the AbstractTabs containing the specified component
  // tabIndex = the index of the tab in tabOwner.
  // Returns (null, -1) if there is no tab owner for this tab component.
  def ownerAndIndexOfTab(tab: Component): (AbstractTabs, Int) = {
    var tabOwner = null.asInstanceOf[AbstractTabs]
    var tabIndex = appTabsPanel.indexOfTabComponent(tab)
    if (tabIndex != -1) {
      tabOwner = appTabsPanel
    } else {
      codeTabsPanelOption match {
        case Some(thePanel) => tabIndex = thePanel.indexOfTabComponent(tab)
          if (tabIndex != -1) {
            tabOwner = thePanel
          }
        case None           =>
      }
    }
    (tabOwner, tabIndex)
  }

  def printAllTabs(): Unit = {
    println("AppTabPanel count " + appTabsPanel.getTabCount)
    printTabsOfTabsPanel(appTabsPanel)
    codeTabsPanelOption match {
      case Some(thePanel) => {
        println("CodeTabs count " + thePanel.getTabCount)
        printTabsOfTabsPanel(thePanel)
      }
      case None           => println("No CodeTabs ")
    }
  }

  def printTabsOfTabsPanel(pane: JTabbedPane): Unit = {
    for (n <- 0 until pane.getTabCount) {
      App.printSwingObject(pane.getComponentAt(n), "")
    }
  }

  // Input: origTabIndx - index a tab would have if there were no separate code tab.
  // Returns (tabOwner, tabComponent)
  // tabOwner = the AbstractTabs containing the indexed tab.
  // tabComponent = the tab in tabOwner referenced by origTabIndx.
  @throws (classOf[IndexOutOfBoundsException])
  def getTabComponentPlus(origTabIndx: Int): (AbstractTabs, Component) = {
    val (tabOwner, tabIndex) = computeIndexPlus(origTabIndx)
    val tabComponent = tabOwner.getTabComponentAt(tabIndex)
    (tabOwner, tabComponent)
  }

  def switchToTabsCodeTab(): Unit = {
    // nothing to do if code tab is already part of Tabs
    val codeTabOwner = getMainCodeTabOwner
    if (!codeTabOwner.isInstanceOf[Tabs]) {
      getAppTabPanel.add(I18N.gui.get("tabs.code"), getAppTabPanel.codeTab)
      codeTabsPanelOption match {
        case Some(theValue) => theValue.getCodeTabContainer.dispose
        case None           =>
      }
      setCodeTabPanel(None)
      getAppTabPanel.codeTab.requestFocus
      // need to remove component, because will no longer exist
      // aab fix this appTabsPanel.getAppFrame.removeLinkComponent(actualMainCodeTabPanel.getCodeTabContainer)
    }
  }

  def switchToSeparateCodeWindow(): Unit = {
    val codeTabOwner = getMainCodeTabOwner
    // Only act if code tab is part of the Tabs panel.
    // Otherwise it is already detached.
    if (codeTabOwner.isInstanceOf[Tabs]) {
      val codeTabsPanel = new MainCodeTabPanel(getAppsTab.workspace,
        getAppsTab.interfaceTab,
        getAppsTab.externalFileManager,
        getAppsTab.codeTab,
        getAppsTab.externalFileTabs)

        // aab maybe some of this should be in an init method shared with
        // MainCodeTabPanel
        codeTabsPanelOption = Some(codeTabsPanel)
        addDeleteCodeTabButton(codeTabsPanel)
        codeTabsPanel.setTabManager(this)
        codeTabsPanel.add(I18N.gui.get("tabs.code"), getAppsTab.codeTab)
        codeTabsPanel.setSelectedComponent(getAppsTab.codeTab)
        getAppsTab.setSelectedComponent(appTabsPanel.interfaceTab)
        appTabsPanel.getAppFrame.addLinkComponent(codeTabsPanel.getCodeTabContainer)
        setSeparateCodeTabBindings(codeTabsPanel)
        // add mouse listener, which should be not set when
        // there is no code tab
      }
  }

  def implementCodeTabSeparationState(isSeparate: Boolean): Unit = {
    if (isSeparate) {
      switchToSeparateCodeWindow
    } else {
      switchToTabsCodeTab
    }
  }

  def addComponentKeys(component: JComponent, key: Int, action: Action, actionName: String): Unit = {
    val inputMap: InputMap = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
    val actionMap: ActionMap = component.getActionMap();
    var mapKey = UserAction.KeyBindings.keystroke(key, withMenu = true, withAlt = false)
    inputMap.put(mapKey, actionName)
    actionMap.put(actionName, action)
    mapKey = UserAction.KeyBindings.keystroke(key, withMenu = true, withAlt = true)
    inputMap.put(mapKey, actionName)
    actionMap.put(actionName, action)
    mapKey = UserAction.KeyBindings.keystroke(key, withMenu = false, withAlt = true)
    inputMap.put(mapKey, actionName)
    actionMap.put(actionName, action)
  }

  def addCodeTabContainerKeys(codeTabsPanel: MainCodeTabPanel, key: Int, action: Action, actionName: String): Unit = {
    val contentPane = codeTabsPanel.getCodeTabContainer.getContentPane.asInstanceOf[JComponent]
    addComponentKeys(contentPane, key, action, actionName)
  }

  def addAppFrameKeys(key: Int, action: Action, actionName: String): Unit = {
    val contentPane = getAppsTab.getAppJFrame.getContentPane.asInstanceOf[JComponent]
    addComponentKeys(contentPane, key, action, actionName)
  }

  def setAppCodeTabBindings(): Unit = {
    addAppFrameKeys(KeyEvent.VK_9, KillSeparateCodeTab, "popInCodeTab")
    addAppFrameKeys(KeyEvent.VK_8, CreateSeparateCodeTab, "popOutCodeTab")
    addAppFrameKeys(KeyEvent.VK_CLOSE_BRACKET, KillSeparateCodeTab, "popInCodeTab")
    addAppFrameKeys(KeyEvent.VK_OPEN_BRACKET, CreateSeparateCodeTab, "popOutCodeTab")
  }

  def setSeparateCodeTabBindings(codeTabsPanel: MainCodeTabPanel): Unit = {
    addCodeTabContainerKeys(codeTabsPanel, KeyEvent.VK_1, SwitchFocusAction1, "switchFocus1")
    addCodeTabContainerKeys(codeTabsPanel, KeyEvent.VK_2, SwitchFocusAction2, "switchFocus2")
    addCodeTabContainerKeys(codeTabsPanel, KeyEvent.VK_9, KillSeparateCodeTab, "popInCodeTab")
    addCodeTabContainerKeys(codeTabsPanel, KeyEvent.VK_CLOSE_BRACKET, KillSeparateCodeTab, "popInCodeTab")
  }

// these objects could also be private classes
  object SwitchFocusAction1 extends AbstractAction("Toggle1") {
    def actionPerformed(e: ActionEvent) {
      // If index is already selected, unselect it
      val index = 0
      val selectedIndex = getSelectedAppTabIndex
      if (selectedIndex == index) {
        setSelectedAppTab(-1)
      }
      setSelectedAppTab(index)
    }
  }

  object SwitchFocusAction2 extends AbstractAction("Toggle2") {
    def actionPerformed(e: ActionEvent) {
      // If index is already selected, unselect it
      val index = 1
      val selectedIndex = getSelectedAppTabIndex
      if (selectedIndex == index) {
        setSelectedAppTab(-1)
      }
      setSelectedAppTab(index)
    }
  }

  object KillSeparateCodeTab extends AbstractAction("PopCodeTabIn") {
    def actionPerformed(e: ActionEvent) {
      switchToTabsCodeTab
    }
  }

  object CreateSeparateCodeTab extends AbstractAction("PopCodeTabOut") {
    def actionPerformed(e: ActionEvent) {
      switchToSeparateCodeWindow
    }
  }

  object Empty extends AbstractAction("Empty") {
    def actionPerformed(e: ActionEvent) {
      // If index is already selected, unselect it
    }
  }

  def addDeleteCodeTabButton(codeTabsPanel: MainCodeTabPanel ): Unit = {
    codeTabsPanel.getCodeTabContainer.getReattachPopOut.addActionListener(KillSeparateCodeTab)
  }
}
