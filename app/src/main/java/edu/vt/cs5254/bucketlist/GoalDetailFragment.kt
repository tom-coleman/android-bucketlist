package edu.vt.cs5254.bucketlist

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import edu.vt.cs5254.bucketlist.databinding.FragmentGoalDetailBinding
import android.text.format.DateFormat
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.view.MenuProvider
import androidx.core.view.doOnLayout
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dalvik.system.PathClassLoader
import kotlinx.coroutines.launch
import java.io.File

private const val DATE_FORMAT = "'Last updated' yyyy-MM-dd 'at' hh:mm:ss a"

class GoalDetailFragment : Fragment() {

    private var _binding: FragmentGoalDetailBinding? = null
    private val binding
        get() = checkNotNull(_binding) { "FragmentGoalDetailBinding is null!" }

    private val args: GoalDetailFragmentArgs by navArgs()
    private val vm: GoalDetailViewModel by viewModels {
        GoalDetailViewModelFactory(args.goalId)
    }

    private val takePhoto = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { didTakePhoto: Boolean ->
        if (didTakePhoto) {
            binding.goalPhoto.tag = null
            vm.goal.value?.let { updatePhoto(it) }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentGoalDetailBinding.inflate(inflater, container, false)

        binding.goalNoteRecyclerView.layoutManager = LinearLayoutManager(context)

        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.fragment_goal_detail, menu)

                val captureImageIntent = takePhoto.contract.createIntent(
                    requireContext(),
                    Uri.EMPTY // NOTE: The "null" used in BNRG is obsolete now
                )
                menu.findItem(R.id.take_photo_menu).isVisible = canResolveIntent(captureImageIntent)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.share_goal_menu -> {
                        vm.goal.value?.let { shareGoal(it) }
                        true
                    }
                    R.id.take_photo_menu -> {
                        vm.goal.value?.let {
                            val photoFile = File(
                                requireContext().applicationContext.filesDir,
                                it.photoFileName
                            )
                            val photoUri = FileProvider.getUriForFile(
                                requireContext(),
                                "edu.vt.cs5254.bucketlist.fileprovider",
                                photoFile
                            )
                            takePhoto.launch(photoUri)
                        }
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

        getItemTouchHelper().attachToRecyclerView(binding.goalNoteRecyclerView)

        binding.pausedCheckbox.setOnClickListener { view: View ->
            vm.updateGoal { oldGoal ->
                oldGoal.copy().apply { notes =
                    if (oldGoal.isPaused) {
                        oldGoal.notes.filter { it.type != GoalNoteType.PAUSED }
                    } else {
                        oldGoal.notes + GoalNote(
                            type = GoalNoteType.PAUSED,
                            goalId = oldGoal.id
                        )
                    }
                }
            }
        }

        binding.completedCheckbox.setOnClickListener { view: View ->
            vm.updateGoal { oldGoal ->
                oldGoal.copy().apply { notes =
                    if (oldGoal.isCompleted) {
                        oldGoal.notes.filter { it.type != GoalNoteType.COMPLETED }
                    } else {
                        oldGoal.notes + GoalNote(
                            type = GoalNoteType.COMPLETED,
                            goalId = oldGoal.id
                        )
                    }
                }
            }
        }

        binding.titleText.doOnTextChanged { text, _, _, _ ->
            vm.updateGoal { oldGoal ->
                oldGoal.copy(title = text.toString()).apply { notes = oldGoal.notes }
            }
        }

        binding.addProgressButton.setOnClickListener {
            findNavController().navigate(
                GoalDetailFragmentDirections.addProgress()
            )
        }

        binding.goalPhoto.setOnClickListener {
            val goal = vm.goal.value
            if (goal != null) {
                findNavController().navigate(
                    GoalDetailFragmentDirections.showImageDetail(goal.photoFileName)
                )
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                vm.goal.collect { goal ->
                    goal?.let { updateView(it) }
                }
            }
        }

        setFragmentResultListener(
            ProgressDialogFragment.REQUEST_KEY_TITLE
        ) { _, bundle ->
            val newNoteTitle = bundle.getString(ProgressDialogFragment.BUNDLE_KEY_TITLE) as String
            vm.updateGoal { oldGoal ->
                oldGoal.copy().apply { notes =
                    oldGoal.notes + GoalNote(
                        type = GoalNoteType.PROGRESS,
                        text = newNoteTitle,
                        goalId = oldGoal.id
                    )
                }
            }
        }
    }

    private fun updateView(goal: Goal) {
        if (binding.titleText.text.toString() != goal.title) {
            binding.titleText.setText(goal.title)
        }

        // Updates the lastUpdatedText if the goal's lastUpdated date has changed
        binding.lastUpdatedText.text = DateFormat.format(DATE_FORMAT, goal.lastUpdated).toString()
        binding.lastUpdatedText.isEnabled = false

        // Managing the checkboxes
        binding.pausedCheckbox.isEnabled = !goal.isCompleted
        binding.pausedCheckbox.isChecked = goal.isPaused

        binding.completedCheckbox.isEnabled = !goal.isPaused
        binding.completedCheckbox.isChecked = goal.isCompleted

        if(goal.isCompleted) binding.addProgressButton.hide() else binding.addProgressButton.show()

        // Display all the notes using a RecyclerView
        binding.goalNoteRecyclerView.adapter = GoalNoteListAdapter(goal.notes)

        updatePhoto(goal)
    }

    private fun updatePhoto(goal: Goal) {
        with(binding.goalPhoto) {
            if (tag != goal.photoFileName) {
                val photoFile =
                    File(requireContext().applicationContext.filesDir, goal.photoFileName)
                if (photoFile.exists()) {
                    doOnLayout { measuredView ->
                        val scaledBM = getScaledBitmap(
                            photoFile.path,
                            measuredView.width,
                            measuredView.height
                        )
                        setImageBitmap(scaledBM)
                        tag = goal.photoFileName
                    }
                    isEnabled = true
                } else {
                    setImageBitmap(null)
                    tag = null
                    isEnabled = false
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getGoalDetails(goal: Goal): String {
        var detailString = ""
        detailString += goal.title
        val dateString = DateFormat.format(DATE_FORMAT, goal.lastUpdated).toString()
        detailString += "\n$dateString"

        // If the goal has progress notes, add them
        val progressNotes = goal.notes.filter { it.type == GoalNoteType.PROGRESS }
        if (progressNotes.isNotEmpty()) {
            val progressLabel = getString(R.string.details_progress)
            detailString += "\n$progressLabel"
            val progressNotesText = progressNotes.joinToString("\n * ") { it.text }
            detailString += "\n * $progressNotesText"
        }

        // If the goal is completed or paused
        if (goal.isCompleted) {
            val completedString = getString(R.string.details_completed)
            detailString += "\n$completedString"
        } else if (goal.isPaused) {
            val pausedString = getString(R.string.details_paused)
            detailString += "\n$pausedString"
        }
        return "$detailString\n"
    }

    private fun shareGoal(goal: Goal) {
        val shareGoal = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, getGoalDetails(goal))
            putExtra(Intent.EXTRA_SUBJECT, goal.title)
        }

        val chooserIntent = Intent.createChooser(
            shareGoal,
            getString(R.string.share_goal)
        )
        startActivity(chooserIntent)
    }

    private fun canResolveIntent(intent: Intent): Boolean {
        val packageManager: PackageManager = requireActivity().packageManager
        val resolvedActivity: ResolveInfo? =
            packageManager.resolveActivity(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
        return resolvedActivity != null

//        return requireActivity().packageManager.resolveActivity(
//            intent,
//            PackageManager.MATCH_DEFAULT_ONLY) != null
    }

    private fun getItemTouchHelper(): ItemTouchHelper {

        return ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, 0) {

            // Override getSwipeDirs to only allow swiping left for only notes of type PROGRESS
            override fun getSwipeDirs(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                val goalNoteHolder = viewHolder as GoalNoteHolder
                return if (goalNoteHolder.boundNote.type == GoalNoteType.PROGRESS) {
                    ItemTouchHelper.LEFT
                } else {
                    0
                }
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = true

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val goalNoteHolder = viewHolder as GoalNoteHolder
                val note = goalNoteHolder.boundNote

                // use vm.updateGoal to remove the note from the goal's notes
                vm.updateGoal { oldGoal ->
                    oldGoal.copy().apply { notes = oldGoal.notes - note }
                }
            }
        })
    }
}