/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.input.gamecontroller

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.settings.R

/**
 * Adapter to show a list view of items provided as a map of <key, String resource>. And it provides
 * a callback on item click, returning the "preference key" of the provided choices that was
 * selected.
 */
class GameControllerRemappingAdapter(
    private val choices: Map</* preference key */ String, Choice>,
    private val currentSelectionKey: String,
    private val onItemClick: (preferenceKey: String) -> Unit,
) : RecyclerView.Adapter<GameControllerRemappingAdapter.ViewHolder>() {

    data class Choice(val name: String, val icon: Drawable)

    private val keys = choices.keys.toList()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconView: ImageView = view.findViewById(R.id.preference_icon)
        val titleView: TextView = view.findViewById(R.id.preference_text)
        val checkIcon: ImageView = view.findViewById(R.id.preference_tick)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.game_controller_remapping_list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val key = keys[position]
        holder.iconView.setImageResource(R.drawable.ic_check_24dp)
        val choice = choices[key]!!
        holder.iconView.setImageDrawable(choice.icon)
        holder.titleView.text = choice.name

        holder.checkIcon.visibility =
            if (key == currentSelectionKey) {
                View.VISIBLE
            } else {
                View.INVISIBLE
            }

        holder.itemView.setOnClickListener { onItemClick(key) }
    }

    override fun getItemCount() = choices.size
}
