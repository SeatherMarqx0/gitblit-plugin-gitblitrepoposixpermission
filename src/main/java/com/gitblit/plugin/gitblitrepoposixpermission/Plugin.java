package com.gitblit.plugin.gitblitrepoposixpermission;

import com.gitblit.extensions.GitblitPlugin;
import ro.fortsoft.pf4j.PluginWrapper;
import ro.fortsoft.pf4j.Version;

public class Plugin extends GitblitPlugin
{

  public Plugin(PluginWrapper wrapper)
  {
    super(wrapper);
  }

  @Override
  public void start()
  {
    log.debug("{} STARTED.", getWrapper().getPluginId());
  }

  @Override
  public void stop()
  {
    log.debug("{} STOPPED.", getWrapper().getPluginId());
  }

  @Override
  public void onInstall()
  {
    log.debug("{} INSTALLED.", getWrapper().getPluginId());
  }

  @Override
  public void onUpgrade(Version oldVersion)
  {
    log.debug("{} UPGRADED from {}.", getWrapper().getPluginId(), oldVersion);
  }

  @Override
  public void onUninstall()
  {
    log.debug("{} UNINSTALLED.", getWrapper().getPluginId());
  }

}