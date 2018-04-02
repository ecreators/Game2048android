package de.ecreators.counter;

import android.app.*;
import android.content.*;
import android.graphics.drawable.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.graphics.*;
import java.util.*;
import android.view.View.*;
import de.ecreators.counter.MainActivity.*;

public class MainActivity extends Activity 
{
	private Grid grid;

	private Button left;

	private Button right;

	private Button up;

	private Button down;

	private int score;

	private TextView scoreView;

	private boolean gameOver;

	private Button reset;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

		score = 0;
		scoreView = (TextView)findViewById(R.id.score);
		gameOver = false;

		View.OnClickListener clickHandler = new OnClickListener(){

			@Override
			public void onClick(View button)
			{
				if (button == reset)
				{
					newGame();
					return;
				}

				if (gameOver)
				{
					return;
				}

				if (button == left)
				{
					onLeft();
				}
				else if (button == right)
				{
					onRight();
				}
				else if (button == up)
				{
					onUp();
				}
				else if (button == down)
				{
					onDown();
				}
			}
		};

		MergeHandler mergeEffectHandler = new MergeHandler() {

			@Override
			public void onMerge(int index, int x, int y, int num, TextView v)
			{
				String txt = "Well done!";
				Toast.makeText(MainActivity.this, txt, 300).show();
				addScore(num);
			}
		};

		left = btn(R.id.left);
		right = btn(R.id.right);
		up = btn(R.id.up);
		down = btn(R.id.down);
		reset = btn(R.id.reset);

		left.setOnClickListener(clickHandler);
		right.setOnClickListener(clickHandler);
		up.setOnClickListener(clickHandler);
		down.setOnClickListener(clickHandler);
		reset.setOnClickListener(clickHandler);

		int gridSize = 4;
		grid = new Grid((GridLayout)findViewById(R.id.grid), gridSize);
		grid.setOnMerge(mergeEffectHandler);

		nextRound(true);
	}

	private void newGame()
	{
		gameOver = false;
		grid.clear();
		addScore(-score);
		setEnabled(true);
		nextRound(true);
	}

	private void addScore(int num)
	{
		score += num;
		scoreView.setText(String.valueOf(score));
	}

	void onLeft()
	{
		nextRound(grid.move(-1, 0));
	}

	void onRight()
	{
		nextRound(grid.move(1, 0));
	}

	void onUp()
	{
		nextRound(grid.move(0, -1));
	}

	void onDown()
	{
		nextRound(grid.move(0, 1));
	}

	private Button btn(int id)
	{
		return (Button)findViewById(id);
	}

	private void nextRound(final boolean moved)
	{
		checkGameOver();
		
		if (gameOver)
		{
			return;
		}
		
		if (moved)
		{
			grid.grid.post(new Runnable() {

					@Override
					public void run()
					{
						grid.putNumber();
					}
				});
		}
		
		grid.grid.post(new Runnable() {

				@Override
				public void run()
				{
					checkGameOver();
				}
			});
	}

	private void checkGameOver()
	{
		if (grid.hasNumber(2048))
		{
			won();
		}
		else if (!grid.checkCanAdd() && !grid.canMerge())
		{
			lose();
		}

		if (gameOver)
		{
			setEnabled(false);
		}
	}

	private void setEnabled(boolean enabled)
	{
		left.setEnabled(enabled);
		right.setEnabled(enabled);
		up.setEnabled(enabled);
		down.setEnabled(enabled);
	}

	private void lose()
	{
		gameOver = true;
		Toast.makeText(this, "Game over!", 1500).show();
	}

	private void won()
	{
		gameOver = true;
		Toast.makeText(this, "Victory!", 3000).show();
	}

	private static class Grid
	{
		private final GridLayout grid;
		private final Cell[][] cells;
		private final Cell[] enumeratedCells;

		private MainActivity.MergeHandler mergeHandler;

		private final int size;

		Grid(GridLayout grid, int size)
		{
			this.size = size;
			this.grid = grid;
			this.grid.setRowCount(size);
			this.grid.setColumnCount(size);
			this.cells = new Cell[size][size];
			this.enumeratedCells = new Cell[size * size];
			createCells();
			fitBest();
		}

		public boolean canMerge()
		{
			for (Cell c: enumeratedCells)
			{
				for (Cell cc: enumeratedCells)
				{
					if (c != cc)
					{
						boolean next = false;
						if (c.column == cc.column - 1)
						{
							next = true;
						}
						else if (c.column == cc.column + 1)
						{
							next = true;
						}
						else if (c.row == cc.row - 1)
						{
							next = true;
						}
						else if (c.row == cc.row + 1)
						{
							next = true;
						}

						if (next && c.number == cc.number && c.number > 0)
						{
							return true;
						}
					}
				}
			}
			return false;
		}

		public void clear()
		{
			for (Cell c : enumeratedCells)
			{
				c.setNum(0);
				c.merged = false;
			}
		}

		public void setOnMerge(MergeHandler mergeEffectHandler)
		{
			this.mergeHandler = mergeEffectHandler;
		}

		public boolean move(int xMove, int yMove)
		{
			resetMerges();

			boolean moved = false;
			for (int i=0;i < size - 1;i++)
			{
				if (xMove > 0)
				{
					moved = moveRight(moved);
				}
				else if (xMove < 0)
				{
					moved = moveLeft(moved);
				}
				else if (yMove > 0)
				{
					moved = moveDown(moved);
				}
				else if (yMove < 0)
				{
					moved = moveUp(moved);
				}
			}
			return moved;
		}

		private boolean moveDown(boolean moved)
		{
			for (int c=0;c < size;c++)
			{
				for (int r=size - 1;r > 0;r--)
				{
					Cell up = cells[c][r - 1];
					Cell below = cells[c][r];

					if (tryMove(up, below))
					{
						moved = true;
					}
				}
			}
			return moved;
		}

		private boolean moveUp(boolean moved)
		{
			for (int c=0;c < size;c++)
			{
				for (int r=0;r < size - 1;r++)
				{
					Cell below = cells[c][r + 1];
					Cell up = cells[c][r];

					if (tryMove(below, up))
					{
						moved = true;
					}
				}
			}
			return moved;
		}

		private boolean moveRight(boolean moved)
		{
			for (int r=0;r < size;r++)
			{
				for (int c=size - 1;c > 0;c--)
				{
					Cell left = cells[c - 1][r];
					Cell right = cells[c][r];

					if (tryMove(left, right))
					{
						moved = true;
					}
				}
			}
			return moved;
		}

		private boolean moveLeft(boolean moved)
		{
			for (int r=0;r < size;r++)
			{
				for (int c=0;c < size - 1;c++)
				{
					Cell right = cells[c + 1][r];
					Cell left = cells[c][r];

					if (tryMove(right, left))
					{
						moved = true;
					}
				}
			}
			return moved;
		}

		private boolean tryMove(Cell from, Cell to)
		{
			if (from.number <= 0 && to.number <= 0)
			{
				return false;
			}

			if (canMerge(from, to))
			{
				to.setNum(to.number * 2);
				to.merged = true;
				from.setNum(0);
				notifyMerge(to.index, to.column, to.row, to.number, to.view);
				return true;
			}
			else if (to.number <= 0)
			{
				to.setNum(from.number);
				from.setNum(0);
				return true;
			}
			return false;
		}

		private void notifyMerge(int index, int column, int row, int number, TextView view)
		{
			MainActivity.MergeHandler handler = mergeHandler;
			if (handler != null)
			{
				handler.onMerge(index, column, row, number, view);
			}
		}

		private void resetMerges()
		{
			for (Cell c : enumeratedCells)
			{
				c.merged = false;
			}
		}

		private static boolean canMerge(Cell a, Cell b)
		{
			boolean can = a.number > 0 && b.number > 0 && a.number == b.number;
			if(can && b.merged)
			{
				return false;
			}
			return can;
		}

		public boolean putNumber()
		{
			boolean canAdd = checkCanAdd();

			if (canAdd)
			{
				int number = randomNumber(new int[] { 2, 4, 8 });
				int i = randomIndex();
				enumeratedCells[i].setNum(number);
			}

			return canAdd;
		}

		private int randomIndex()
		{
			List<Integer> empty = new ArrayList<Integer>();
			for (int i=0;i < enumeratedCells.length;i++)
			{
				if (enumeratedCells[i].number <= 0)
				{
					empty.add(i);
				}
			}
			Random random = new Random(UUID.randomUUID().hashCode());
			int i = random.nextInt(empty.size());
			return empty.get(i);
		}

		private static int randomNumber(int[] pool)
		{
			Random rand = new Random(UUID.randomUUID().hashCode());
			return pool[rand.nextInt(pool.length)];
		}

		private boolean checkCanAdd()
		{
			for (Cell c : enumeratedCells)
			{
				if (c.number <= 0)
				{
					return true;
				}
			}
			return false;
		}

		public boolean hasNumber(int num)
		{
			for (Cell c : enumeratedCells)
			{
				if (c.number == num)
				{
					return true;
				}
			}

			return false;
		}

		private void createCells()
		{
			for (int y =0, i=0;y < size;y++)
			{
				for (int x=0;x < size;x++,i++)
				{
					Cell cell = new Cell(this, x, y);
					this.cells[x][y] = cell;
					this.enumeratedCells[i] = cell;
				}
			}
		}

		public void fitBest()
		{
			for (Cell c : enumeratedCells)
			{
				c.setNum(9999);
			}
			
			grid.post(new Runnable() {

					@Override
					public void run()
					{
						for (Cell c : enumeratedCells)
						{
							c.fitBest();
						}
					}
				});
			grid.post(new Runnable() {

					@Override
					public void run()
					{
						for (Cell c : enumeratedCells)
						{
							c.setNum(0);
						}
					}
				});
		}
	}

	public static class Cell
	{
		private final Grid grid;
		private final TextView view;
		private final int index;
		public final int row;
		public final int column;

		private int bg;
		private final int[] nbg;

		public int number;

		public boolean merged;

		Cell(Grid g, int x, int y)
		{
			this.grid = g;
			Context context = g.grid.getContext();

			this.view = new TextView(context);
			view.setGravity(Gravity.CENTER);

			int th = Math.round(7 * view.getResources().getDisplayMetrics().density);
			view.setPadding(th, th, th, th);

			index = this.grid.grid.getChildCount();
			grid.grid.addView(view);

			row = y;
			column = x;

			bg = Color.WHITE;

			addBorder();

			nbg = new int[] {
				Color.WHITE, // 2
				Color.rgb(255, 204, 153), // 4
				Color.rgb(255, 255, 153), // 8
				Color.rgb(204, 255, 102), // 16
				Color.rgb(255, 153, 153), // 32
				Color.rgb(204, 204, 255), // 64
				Color.rgb(102, 204, 255), // 128
				Color.rgb(204, 102, 255), // 256
				Color.rgb(255, 255, 0), // 512
				Color.rgb(153, 204, 0), // 1024
				Color.rgb(255, 51, 0), // 2048
			};
		}

		private void addBorder()
		{
			//use a GradientDrawable with only one color set, to make it a solid color
			GradientDrawable border = new GradientDrawable();
			border.setColor(bg); //white background
			border.setStroke(1, 0xFF000000); //black border with full opacity
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
			{
				view.setBackgroundDrawable(border);
			}
			else
			{
				view.setBackground(border);
			}
		}

		private void updateBackgroundColor()
		{
			bg = nbg[0];
			if (number > 0)
			{
				int exp = Math.getExponent(number);
				int i = (exp - 1) % nbg.length;
				if (i >= 0 && i < nbg.length)
				{
					bg = nbg[i];
				}
			}
			addBorder();
		}

		public void fitBest()
		{
			view.post(new Runnable() {

					@Override
					public void run()
					{
						int w = view.getWidth();
						int h = view.getHeight();
						postFitBest(w, h);
					}

					private void postFitBest(int w, int h)
					{
						ViewGroup.LayoutParams p = view.getLayoutParams();
						if (w > h)
						{
							p.width = w;
							p.height = w;
							view.setLayoutParams(p);
						}
						else if (h > w)
						{
							p.width = h;
							p.height = h;
							view.setLayoutParams(p);
						}
					}
				});
		}

		public void setNum(final int number)
		{
			this.number = number;
			view.post(new Runnable()
				{

					@Override
					public void run()
					{
						if (number <= 0)
						{
							view.setText(null);
						}
						else
						{
							view.setText(String.valueOf(number));
						}

						updateBackgroundColor();
					}
				});
		}
	}

	public static interface MergeHandler
	{
		void onMerge(int index, int x, int y, int num, TextView v);
	}
}
