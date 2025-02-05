import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.core.view.doOnLayout
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import edu.vt.cs5254.bucketlist.databinding.FragmentImageDialogBinding
import edu.vt.cs5254.bucketlist.getScaledBitmap
import java.io.File

class ImageDialogFragment : DialogFragment() {

    private val args: ImageDialogFragmentArgs by navArgs()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = FragmentImageDialogBinding.inflate(layoutInflater)

        val imageFile = File(requireContext().applicationContext.filesDir, args.goalImageFilename)

        binding.root.doOnLayout { measuredView ->
            val scaledBitmap = getScaledBitmap(
                imageFile.path,
                measuredView.width,
                measuredView.height
            )
            binding.imageDetail.setImageBitmap(scaledBitmap)
        }

        return AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .show()
    }
}