package TSX.telkom.final_project.main.Adapter
import TSX.telkom.final_project.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ViewPagerAdapter(
    private var imageUrls: List<String>,
    private val onImageClick: (Int) -> Unit) :
    RecyclerView.Adapter<ViewPagerAdapter.ViewHolder>() {

    fun updateImages(newImageUrls: List<String>) {
        this.imageUrls = newImageUrls
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = imageUrls.size.coerceAtMost(5)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_view, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val imageUrl = imageUrls.getOrNull(position) ?: return
        Glide.with(holder.imageView.context)
            .load(imageUrl)
            .placeholder(R.drawable.load)
            .error(R.drawable.error)
            .into(holder.imageView)

        holder.imageView.setOnClickListener {
            onImageClick(position)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
    }
}