package com.android.email.preferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.android.email.Email;

import android.util.Log;

public class Editor implements android.content.SharedPreferences.Editor
{
  private Storage storage;
  private HashMap<String, String> changes = new HashMap<String, String>();
  private ArrayList<String> removals = new ArrayList<String>();
  private boolean removeAll = false;
  
  Map<String, String> snapshot = new HashMap<String, String>();
  
  
  protected Editor(Storage storage)
  {
    this.storage = storage;
    snapshot.putAll(storage.getAll());
  }
  
  public void copy(android.content.SharedPreferences input)
  {
    Map<String, ?> oldVals = input.getAll();
    for (Entry<String, ?> entry : oldVals.entrySet())
    {
      String key = entry.getKey();
      Object value = entry.getValue();
      if (key != null && value != null)
      {
        if (Email.DEBUG)
        {
          Log.d(Email.LOG_TAG, "Copying key '" + key + "', value '" + value + "'");
        }
        changes.put(key, "" + value);
      }
      else
      {
        if (Email.DEBUG)
        {
          Log.d(Email.LOG_TAG, "Skipping copying key '" + key + "', value '" + value + "'");
        }
      }
    }
  }
  
  @Override
  public android.content.SharedPreferences.Editor clear()
  {
    removeAll = true;
    return this;
  }

  /* This method is poorly defined.  It should throw an Exception on failure */
  @Override
  public boolean commit()
  {
    try
    {
      commitChanges();
      return true;
    }
    catch (Exception e)
    {
      Log.e(Email.LOG_TAG, "Failed to save preferences", e);
      return false;
    }
  }
  
  public void commitChanges() throws Exception
  {
    long startTime = System.currentTimeMillis();
    Log.i(Email.LOG_TAG, "Committing preference changes");
    Runnable committer = new Runnable() {
      public void run()
      {
        if (removeAll)
        {
          storage.removeAll();
        }
        for (String removeKey : removals)
        {
          storage.remove(removeKey);
        }
        for (Entry<String, String> entry : changes.entrySet())
        {
          String key = entry.getKey();
          String newValue = entry.getValue();
          String oldValue = snapshot.get(key);
          if (removeAll || removals.contains(key) || newValue.equals(oldValue) != true)
          {
            storage.put(key, newValue);
          }
        }
      }
    };
    storage.doInTransaction(committer);
    long endTime = System.currentTimeMillis();
    Log.i(Email.LOG_TAG, "Preferences commit took " + (endTime - startTime) + "ms");
    
  }

  @Override
  public android.content.SharedPreferences.Editor putBoolean(String key,
      boolean value)
  {
    changes.put(key, "" + value);
    return this;
  }

  @Override
  public android.content.SharedPreferences.Editor putFloat(String key,
      float value)
  {
    changes.put(key, "" + value);
    return this;
  }

  @Override
  public android.content.SharedPreferences.Editor putInt(String key, int value)
  {
    changes.put(key, "" + value);
    return this;
  }

  @Override
  public android.content.SharedPreferences.Editor putLong(String key, long value)
  {
    changes.put(key, "" + value);
    return this;
  }

  @Override
  public android.content.SharedPreferences.Editor putString(String key,
      String value)
  {
    if (value == null)
    {
      remove(key);
    }
    else
    {
      changes.put(key, value);
    }
    return this;
  }

  @Override
  public android.content.SharedPreferences.Editor remove(String key)
  {
    removals.add(key);
    return this;
  }

}
