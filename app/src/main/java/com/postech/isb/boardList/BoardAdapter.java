package com.postech.isb.boardList;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.util.Log;
import android.view.*;
import android.view.View.OnClickListener;
import com.postech.isb.R;
import com.postech.isb.R.id;
import android.widget.ImageView;

import java.util.*;

public class BoardAdapter extends ArrayAdapter<Board> implements Filterable {
	private Context context;
	private int resource;
	private ArrayList<Board> mDataShown;
	private ArrayList<Board> mAllData;
	
	private BoardFilter mFilter;
	
	private HashMap<Character, Integer> fstIdx;
		
	public BoardAdapter(Context _context, int _resource, List<Board> _items) {
		super(_context, _resource, _items);
		mDataShown = (ArrayList<Board>)_items;
		mAllData = new ArrayList<Board>();
		mAllData.addAll(mDataShown);		
		context = _context;
		resource = _resource;
		
		fstIdx = new HashMap<Character, Integer>();
		
		Character a;
		fstIdx.clear();
		for (int i = mAllData.size() - 1; i >= 0; i--) {
			a = Character.toLowerCase(mAllData.get(i).name.charAt(0));
			fstIdx.put(a, i);
		}
	}
	
	@Override
	public void clear() {
		super.clear();
		mAllData.clear();
	}
	
	@Override
	public void add(Board item) {
		super.add(item);
		mAllData.add(item);
	}
	
	@Override
	public void notifyDataSetChanged() {
		super.notifyDataSetChanged();

		Character a;
		fstIdx.clear();
		for (int i = mAllData.size() - 1; i >= 0; i--) {
			a = Character.toLowerCase(mAllData.get(i).name.charAt(0));
			fstIdx.put(a, i);
		}

	}
	
	public void toggleFavorite(int index) {
		Board t = getItem(index);
		t.favorite = !t.favorite;
		mDataShown.set(index, t);
		mAllData.set(mAllData.indexOf(t), t);
		Log.i("debug", "State : " + t.favorite);
		super.notifyDataSetChanged();
	}
	
	public void toggleMyboard(int index) {
		Board t = getItem(index);
		boolean myboard = !t.myBoard;
		
		Log.i("debug", "mDataShown : " + mDataShown.size());
		for (int i = 0; i < mDataShown.size(); i++){
			Board t_other = getItem(i);
			t_other.myBoard = false;
			mDataShown.set(i, t_other);
			mAllData.set(mAllData.indexOf(t_other), t_other);
		}
		
		t.myBoard = myboard;
		mDataShown.set(index, t);
		mAllData.set(mAllData.indexOf(t), t);
		super.notifyDataSetChanged();
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View boardView = convertView;

		if (convertView == null) {
			LayoutInflater vi = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			boardView = vi.inflate(resource, null);
		}

		Board item;
//		try {
			item = getItem(position);
//		} catch (IndexOutOfBoundsException e) {
//			item = null;		
//		}

		if (item != null) {
			String boardName = item.name;
			boolean isFavorite = item.favorite;
			//ImageView test = (ImageView)boardView.findViewById(R.id.BoardListNew);
			//test.setVisibility(View.VISIBLE);

			((ImageView)boardView.findViewById(R.id.BoardListNew)).setVisibility(item.newt ? View.VISIBLE : View.GONE);
			((ImageView)boardView.findViewById(R.id.BoardListComment)).setVisibility(item.comment ? View.VISIBLE : View.GONE);
			((CheckBox)boardView.findViewById(R.id.favorite)).setChecked(isFavorite);
			((TextView)boardView.findViewById(R.id.BoardListTitle)).setText(boardName);
			((CheckBox)boardView.findViewById(R.id.favorite)).setClickable(false);
			if( item.myBoard )
			{
				((CheckBox)boardView.findViewById(R.id.favorite)).setButtonDrawable(R.drawable.btn_myboard);
			}
			else
				((CheckBox)boardView.findViewById(R.id.favorite)).setButtonDrawable(R.drawable.btn_star);
			
		}

		return boardView;
	}

	@Override
	public Filter getFilter(){
		if (mFilter == null)
			mFilter = new BoardFilter();
		
		return mFilter;
	}	
		
	private class BoardFilter extends Filter {
		@Override
		public String convertResultToString(Object resultValue) {
			return ((Board)resultValue).name;
		}

		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			FilterResults filterResults = new FilterResults();
			if (constraint != null && constraint.length() > 0) {
				ArrayList<Board> tmpAllData = new ArrayList<Board>();
				ArrayList<Board> tmpDataShown = new ArrayList<Board>();

				tmpAllData.addAll(mAllData);
				
				Character fst = Character.toLowerCase(constraint.charAt(0));
				Integer startIdx = fstIdx.get(fst);

				if (startIdx != null)
				{
					boolean noSecond = constraint.length() == 1;
					for (int i = startIdx.intValue(); i < tmpAllData.size(); i++)
					{
						if (!tmpAllData.get(i).name.matches("^(?i)"+fst+".*"))
							break;

						if (noSecond || tmpAllData.get(i).name.matches("(?i).*?/"+constraint.charAt(1)+".*"))
							tmpDataShown.add(tmpAllData.get(i));
					}
				}
				filterResults.values = tmpDataShown;
				filterResults.count = tmpDataShown.size();				
			}
			else {
				filterResults.values = mAllData;
				filterResults.count = mAllData.size();
			}

			return filterResults;
		}

		@Override
		protected void publishResults(CharSequence constraint, FilterResults results) {
			if (results != null) {
				synchronized (this) {
					mDataShown.clear();
					mDataShown.addAll((ArrayList<Board>)results.values);
				}
				notifyDataSetChanged();
			}
		}
	}
}
