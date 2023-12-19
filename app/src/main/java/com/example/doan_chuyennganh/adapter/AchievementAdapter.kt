import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.doan_chuyennganh.achievement.Achievements
import com.example.doan_chuyennganh.R

class AchievementAdapter(
    private val achievements: List<Achievements>,
    private val onAchievementClickListener: (Achievements) -> Unit
) : RecyclerView.Adapter<AchievementAdapter.AchievementViewHolder>() {

    class AchievementViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvTitle)
        val tvDescription: TextView = view.findViewById(R.id.tvDescription)
        val progressBar: ProgressBar = view.findViewById(R.id.progressBar)
        val tvProgress: TextView = view.findViewById(R.id.tvProgress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AchievementViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_achievement, parent, false)
        return AchievementViewHolder(view)
    }

    override fun onBindViewHolder(holder: AchievementViewHolder, position: Int) {
        val achievement = achievements[position]
        holder.tvTitle.text = achievement.title
        holder.tvDescription.text = achievement.description
        holder.progressBar.max = achievement.goal
        holder.progressBar.progress = achievement.currentCount
        holder.tvProgress.text = "${achievement.currentCount}/${achievement.goal}"

        // Thiết lập click listener cho mỗi item
        holder.itemView.setOnClickListener {
            onAchievementClickListener(achievement)
        }
    }

    override fun getItemCount() = achievements.size
}
