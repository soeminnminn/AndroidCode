package com.s16.data;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.TextView;

public class JSONArrayAdapter extends BaseAdapter {
	
	protected Context mContext;
	protected final Handler mHandler = new Handler(Looper.getMainLooper());
	
	protected LayoutInflater mInflater;
	
	/**
     * The resource indicating what views to inflate to display the content of this
     * array adapter.
     */
	protected int mResource;

    /**
     * The resource indicating what views to inflate to display the content of this
     * array adapter in a drop down widget.
     */
	protected int mDropDownResource;
    /**
     * If the inflated resource is not a TextView, {@link #mFieldId} is used to find
     * a TextView inside the inflated views hierarchy. This field must contain the
     * identifier that matches the one defined in the resource file.
     */
	protected int mFieldId = 0;    
	
	/**
     * This field should be made private, so it is hidden from the SDK.
     * {@hide}
     */
    protected boolean mDataValid;
    protected long mUiThreadId = -1;
    
	protected JSONArray mObjects;
	protected String mIdField;
	protected String mTextField;
	
	public interface JSONArrayFilterProvider {
		JSONArray runQuery(CharSequence constraint);
	}
	
	class JSONArrayFilter extends Filter {

		@Override
	    public CharSequence convertResultToString(Object resultValue) {
	        return convertToString((JSONArray)resultValue);
	    }
		
		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			JSONArray jsonArray = runQueryOnBackgroundThread(constraint);

	        FilterResults results = new FilterResults();
	        if (jsonArray != null) {
	            results.count = jsonArray.length();
	            results.values = jsonArray;
	        } else {
	            results.count = 0;
	            results.values = null;
	        }
	        return results;
		}

		@Override
		protected void publishResults(CharSequence constraint, FilterResults results) {
			JSONArray oldJsonArray = getJSONArray();
	        
	        if (results.values != null && results.values != oldJsonArray) {
	        	changeSource((JSONArray)results.values);
	        }
		}

	}
	
	/**
     * This field should be made private, so it is hidden from the SDK.
     * {@hide}
     */
    protected JSONArrayFilter mFilter;
    /**
     * This field should be made private, so it is hidden from the SDK.
     * {@hide}
     */
    protected JSONArrayFilterProvider mFilterProvider;
    
    /**
     * Constructor
     *
     * @param context The current context.
     * @param resource The resource ID for a layout file containing a layout to use when
     *                 instantiating views.
     * @param idField The field name of the id.
     * @param textField The field name of the display text.
     */
    public JSONArrayAdapter(Context context, int resource, String idField, String textField) {
    	init(context, resource, 0, null, idField, textField);
    }
    
    /**
     * Constructor
     *
     * @param context The current context.
     * @param resource The resource ID for a layout file containing a layout to use when
     *                 instantiating views.
     * @param textViewResourceId The id of the TextView within the layout resource to be populated
     * @param idField The field name of the id.
     * @param textField The field name of the display text.
     */
    public JSONArrayAdapter(Context context, int resource, int textViewResourceId, String idField, String textField) {
    	init(context, resource, textViewResourceId, null, idField, textField);
    }
    
    /**
     * Constructor
     *
     * @param context The current context.
     * @param resource The resource ID for a layout file containing a layout to use when
     *                 instantiating views.
     * @param array The JSONArray to represent in the ListView.
     * @param idField The field name of the id.
     * @param textField The field name of the display text.
     */
	public JSONArrayAdapter(Context context, int resource, JSONArray array, String idField, String textField) {
		init(context, resource, 0, array, idField, textField);
	}
	
	/**
     * Constructor
     *
     * @param context The current context.
     * @param resource The resource ID for a layout file containing a layout to use when
     *                 instantiating views.
     * @param textViewResourceId The id of the TextView within the layout resource to be populated
     * @param array The JSONArray to represent in the ListView.
     * @param idField The field name of the id.
     * @param textField The field name of the display text.
     */
	public JSONArrayAdapter(Context context, int resource, int textViewResourceId, JSONArray array, String idField, String textField) {
		init(context, resource, textViewResourceId, array, idField, textField);
	}
	
	protected void init(Context context, int resource, int textViewResourceId, JSONArray objects, String idField, String textField) {
        mContext = context;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mResource = mDropDownResource = resource;
        mObjects = objects;
        mDataValid = (mObjects != null);
        mFieldId = textViewResourceId;
        mIdField = idField;
        mTextField = textField;
        if (Thread.currentThread() != null) {
        	mUiThreadId = Thread.currentThread().getId();
        }
    }
	
	/**
     * Returns the context associated with this array adapter. The context is used
     * to create views from the resource passed to the constructor.
     *
     * @return The Context associated with this adapter.
     */
	protected Context getContext() {
        return mContext;
    }
	
	/**
     * Returns the JSONArray.
     * @return the JSONArray.
     */
	public JSONArray getJSONArray() {
		return mObjects;
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public int getCount() {
		if (mDataValid) {
			return mObjects.length();
		}
		return 0;
	}

	/**
     * {@inheritDoc}
     */
	@Override
	public Object getItem(int position) {
		if (mDataValid) {
			Object item = null;
			try {
				item = mObjects.get(position);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			if (item != null) {
	        	if (item instanceof JSONObject) {
	            	return (JSONObject)item;
	        	}
	        }
		}
		return null;
	}

	/**
     * {@inheritDoc}
     */
	@Override
	public long getItemId(int position) {
		Object item = getItem(position);
		long id = position;
        if ((item != null) && (item instanceof JSONObject)) {
        	JSONObject jsonObject = (JSONObject)item;
            try {
            	id = jsonObject.getLong(mIdField);
			} catch (JSONException e) {
				e.printStackTrace();
			}
        }
		return id;
	}
	
	/**
     * Returns the JSONObject.
     * @return the JSONObject.
     */
	public JSONObject getJSONObject(int position) {
		if (mDataValid) {
			JSONObject item = null;
			try {
				item = mObjects.getJSONObject(position);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			return item;
		}
        return null;
	}
	
	/**
     * Returns the JSONArray.
     * @return the JSONArray.
     */
	public JSONArray getJSONArray(int position) {
		if (mDataValid) {
			JSONArray item = null;
			try {
				item = mObjects.getJSONArray(position);
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
			return item;
		}
		return null;
	}
	
	/**
     * @see android.widget.ListAdapter#getView(int, View, ViewGroup)
     */
    public View getView(int position, View convertView, ViewGroup parent) {
        if (!mDataValid) {
            throw new IllegalStateException("this should only be called when the JSONObject is valid");
        }
        if ((mUiThreadId == -1) && (Thread.currentThread() != null)) {
        	mUiThreadId = Thread.currentThread().getId();
        }
        
        JSONObject dataItem = getJSONObject(position);
        View v;
        if (convertView == null) {
            v = newView(mContext, dataItem, parent);
        } else {
            v = convertView;
        }
        bindView(v, mContext, dataItem);
        return v;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        if (mDataValid) {
        	JSONObject dataItem = getJSONObject(position);
            View v;
            if (convertView == null) {
                v = newDropDownView(mContext, dataItem, parent);
            } else {
                v = convertView;
            }
            bindView(v, mContext, dataItem);
            return v;
        } else {
            return null;
        }
    }
	
	/**
     * Makes a new view to hold the data pointed to by cursor.
     * @param context Interface to application's global information
     * @param jsonObject The JSONObject from which to get the data. The cursor is already
     * moved to the correct position.
     * @param parent The parent to which the new view is attached to
     * @return the newly created view.
     */
	protected View newView(Context context, JSONObject jsonObject, ViewGroup parent) {
		return mInflater.inflate(mResource, parent, false);
	}
	
	/**
     * Makes a new drop down view to hold the data pointed to by cursor.
     * @param context Interface to application's global information
     * @param jsonObject The JSONObject from which to get the data. The cursor is already
     * moved to the correct position.
     * @param parent The parent to which the new view is attached to
     * @return the newly created view.
     */
	protected View newDropDownView(Context context, JSONObject jsonObject, ViewGroup parent) {
		return mInflater.inflate(mDropDownResource, parent, false);
    }
	
    /**
     * Bind an existing view to the data pointed to by cursor
     * @param view Existing view, returned earlier by newView
     * @param context Interface to application's global information
     * @param jsonObject The JSONObject from which to get the data. The cursor is already
     * moved to the correct position.
     */
	protected void bindView(View view, Context context, JSONObject jsonObject) {
		TextView text;
		try {
            if (mFieldId == 0) {
                //  If no custom field is assigned, assume the whole resource is a TextView
                text = (TextView)view;
            } else {
                //  Otherwise, find the TextView field within the layout
                text = (TextView)view.findViewById(mFieldId);
            }
        } catch (ClassCastException e) {
            Log.e("JSONArrayAdapter", "You must supply a resource ID for a TextView");
            throw new IllegalStateException(
                    "JSONArrayAdapter requires the resource ID to be a TextView", e);
        }

        if (jsonObject != null) {
        	try {
				text.setText(jsonObject.get(mTextField).toString());
			} catch (JSONException e) {
				e.printStackTrace();
			}
        }
	}
	
	/**
     * <p>Sets the layout resource to create the drop down views.</p>
     *
     * @param resource the layout resource defining the drop down views
     * @see #getDropDownView(int, android.view.View, android.view.ViewGroup)
     */
    public void setDropDownViewResource(int resource) {
        this.mDropDownResource = resource;
    }
    
    /**
     * <p>Converts the cursor into a CharSequence. Subclasses should override this
     * method to convert their results. The default implementation returns an
     * empty String for null values or the default String representation of
     * the value.</p>
     *
     * @param jsonArray the JSONArray to convert to a CharSequence
     * @return a CharSequence representing the value
     */
    public CharSequence convertToString(JSONArray jsonArray) {
        return jsonArray == null ? "" : jsonArray.toString();
    }
    
    /**
     * Runs a query with the specified constraint. This query is requested
     * by the filter attached to this adapter.
     *
     * The query is provided by a
     * {@link JSONArrayFilterProvider}.
     * If no provider is specified, the current cursor is not filtered and returned.
     *
     * After this method returns the resulting JSONArray is passed to {@link #changeSource(JSONArray)}
     *
     * This method is always executed on a background thread, not on the
     * application's main thread (or UI thread.)
     * 
     * Contract: when constraint is null or empty, the original results,
     * prior to any filtering, must be returned.
     *
     * @param constraint the constraint with which the query must be filtered
     *
     * @return a JSONArray representing the results of the new query
     *
     * @see #getFilter()
     * @see #getJSONArrayFilterProvider()
     * @see #setJSONArrayFilterProvider(JSONArrayFilterProvider)
     */
    public JSONArray runQueryOnBackgroundThread(CharSequence constraint) {
        if (mFilterProvider != null) {
            return mFilterProvider.runQuery(constraint);
        }

        return mObjects;
    }

    public Filter getFilter() {
        if (mFilter == null) {
        	mFilter = new JSONArrayFilter();
        }
        return mFilter;
    }

    /**
     * Returns the query filter provider used for filtering. When the
     * provider is null, no filtering occurs.
     *
     * @return the current filter query provider or null if it does not exist
     *
     * @see #setJSONArrayFilterProvider(JSONArrayFilterProvider)
     * @see #runQueryOnBackgroundThread(CharSequence)
     */
    public JSONArrayFilterProvider getJSONArrayFilterProvider() {
        return mFilterProvider;
    }

    /**
     * Sets the query filter provider used to filter the current Cursor.
     * The provider's
     * {@link JSONArrayFilterProvider#runQuery(CharSequence)}
     * method is invoked when filtering is requested by a client of
     * this adapter.
     *
     * @param filterProvider the filter query provider or null to remove it
     *
     * @see #getJSONArrayFilterProvider()
     * @see #runQueryOnBackgroundThread(CharSequence)
     */
    public void setJSONArrayFilterProvider(JSONArrayFilterProvider filterProvider) {
        mFilterProvider = filterProvider;
    }
    
    /**
     * Change the underlying JSONArray to a new JSONArray. 
     * 
     * @param jsonArray The new JSONArray to be used
     */
    public void changeSource(final JSONArray jsonArray) {
    	if (mUiThreadId == Thread.currentThread().getId()) {
    		mObjects = jsonArray;
        	mDataValid = (mObjects != null);
            notifyDataSetChanged();
    	} else {
	    	// Enqueue work on mHandler to change the data on the main thread.
	        mHandler.post(new Runnable() {
	            @Override
	            public void run() {
	            	mObjects = jsonArray;
	            	mDataValid = (mObjects != null);
	                notifyDataSetChanged();
	            }
	        });
    	}
    }
}
