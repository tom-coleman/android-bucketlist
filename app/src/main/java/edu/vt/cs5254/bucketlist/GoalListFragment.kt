package edu.vt.cs5254.bucketlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import edu.vt.cs5254.bucketlist.databinding.FragmentGoalListBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

private const val TAG = "GoalListFragment"

class GoalListFragment : Fragment() {

    private var _binding: FragmentGoalListBinding? = null
    private val binding
        get() = checkNotNull(_binding) { "FragmentGoalListBinding is null!" }

    private val goalListViewModel: GoalListViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentGoalListBinding.inflate(inflater, container, false)

        binding.goalRecyclerView.layoutManager = LinearLayoutManager(context)

        requireActivity().addMenuProvider(object : MenuProvider {
            // blank for now
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.fragment_goal_list, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.new_goal -> {
                        showNewGoal()
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getItemTouchHelper().attachToRecyclerView(binding.goalRecyclerView)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                goalListViewModel.goals.collect { goals ->
                    // Display all the goals, or empty goal list with the ability to add new goal
                    if (goals.isEmpty()) {
                        binding.noGoalText.visibility = View.VISIBLE
                        binding.noGoalButton.visibility = View.VISIBLE
                        binding.goalRecyclerView.visibility = View.GONE
                    } else {
                        binding.noGoalText.visibility = View.GONE
                        binding.noGoalButton.visibility = View.GONE
                        binding.goalRecyclerView.visibility = View.VISIBLE
                    }

                    binding.noGoalButton.setOnClickListener {
                        showNewGoal()
                    }

                    binding.goalRecyclerView.adapter = GoalListAdapter(goals) { goalId ->
                        findNavController().navigate(
                            GoalListFragmentDirections.showGoalDetail(goalId)
                        )
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showNewGoal() {
        viewLifecycleOwner.lifecycleScope.launch {
            val newGoal = Goal()
            goalListViewModel.addGoal(newGoal)
            findNavController().navigate(
                GoalListFragmentDirections.showGoalDetail(newGoal.id)
            )
        }
    }

    private fun getItemTouchHelper(): ItemTouchHelper {

        return ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = true

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val goalHolder = viewHolder as GoalHolder
                val goal = goalHolder.boundGoal
                goalListViewModel.deleteGoal(goal)
            }
        })
    }
}