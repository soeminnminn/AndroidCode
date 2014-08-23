package com.s16.data;

import java.util.HashMap;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.text.TextUtils;

public class DataTable {
	
	@SuppressLint("UseSparseArrays")
	private final HashMap<String, DataColumn> mColumns = new HashMap<String, DataColumn>();
	private final String mUriString;
	private final String mName;
	
	public static DataTable newInstance(String uriString, String tableName) {
		return new DataTable(uriString, tableName); 
	}
	
	public DataTable(String uriString, String tableName) {
		mUriString = uriString;
		mName = tableName;
	}
	
	public String getUriString() {
		return mUriString;
	}
	
	public Uri getUri() {
		if (TextUtils.isEmpty(mUriString)) return null;
		return Uri.parse(mUriString + "/" + mName);
	}
	
	public String getTableName() {
		return mName;
	}
	
	public DataTable addColumn(DataColumn column) {
		mColumns.put(column.name, column);
		return this;
	}
	
	public DataTable addColumn(String columnName, String sqlDataType) {
		return addColumn(columnName, sqlDataType, false, true, false);
	}
	
	public DataTable addColumn(String columnName, String sqlDataType, boolean isAllowNull) {
		return addColumn(columnName, sqlDataType, false, isAllowNull, false);
	}
	
	public DataTable addColumn(String columnName, String sqlDataType, boolean isPrimaryKey, boolean isAllowNull, boolean isAutoIncrement) {
		DataColumn column = new DataColumn(columnName, sqlDataType);
		column.isPrimaryKey = isPrimaryKey;
		column.isAllowNull = isAllowNull;
		column.isAutoIncrement = isAutoIncrement;
		return addColumn(column);
	}
	
	public DataColumn getColumn(String columnName) {
		if (mColumns.containsKey(columnName)) {
			return mColumns.get(columnName);
		}
		return null;
	}
	
	public DataColumn getPrimaryKey() {
		if (mColumns == null || mColumns.size() == 0) return null;
		DataColumn keyColumn = null;
		
		for(DataColumn column : mColumns.values()) {
			if (column.isPrimaryKey) {
				keyColumn = column;
				break;
			}
		}
		
		return keyColumn;
	}
	
	public String[] getColumnNames() {
		if (mColumns == null || mColumns.size() == 0) return null;
		String[] columns = new String[mColumns.size()];
		mColumns.keySet().toArray(columns);
		return columns;
	}
	
	public int getColumnCount() {
    	return mColumns.size();
    }
	
	public String createStatement(boolean withIsNotExist) {
		if (mColumns == null || mColumns.size() == 0) return "";
		String sql = "";
		sql += "CREATE TABLE ";
		if (withIsNotExist) {
			sql += "IF NOT EXISTS ";
		}
		sql += "`" + mName + "` (";
		
		boolean isFirst = true;
		for(DataColumn column : mColumns.values()) {
			
			if (!isFirst)sql += ",";
			isFirst = false;
			
			sql += " `" + column.name + "`";
			sql += " " + column.dataType;
			if (column.isPrimaryKey) {
				sql += " PRIMARY KEY";
			}
			if (!column.isAllowNull) {
				sql += " NOT NULL";
			}
			if (column.isAutoIncrement) {
				sql += " AUTOINCREMENT";
			}
		}
		
		sql += ");";
		
		return sql;
	}
	
	public String dropStatement(boolean withIsExist) {
		return "DROP TABLE " + (withIsExist ? "IF EXISTS " : "") + "`" + mName + "`);";
	}
	
	public class DataColumn {
		
		public String name;
		public String dataType;
		public boolean isPrimaryKey;
		public boolean isAllowNull;
		public boolean isAutoIncrement;
		
		public DataColumn(String columnName, String sqlDataType) {
			name = columnName;
			dataType = sqlDataType;
			isAllowNull = true;
		}
	}
}
