package com.uitm.smartsportvenuefinder;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class StepAdapter extends RecyclerView.Adapter<StepAdapter.ViewHolder> {

    private List<String> instructions;
    private List<String> distances;
    private int currentStep = -1;

    public StepAdapter(List<String> instructions, List<String> distances) {
        this.instructions = instructions;
        this.distances = distances;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_navigation_step, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String instruction = instructions.get(position);
        if (instruction.length() > 50) {
            instruction = instruction.substring(0, 47) + "...";
        }
        holder.tvInstruction.setText(instruction);

        if (distances != null && position < distances.size()) {
            holder.tvDistance.setText(distances.get(position));
        }

        if (position == currentStep) {
            holder.itemView.setBackgroundColor(holder.itemView.getContext()
                    .getResources().getColor(android.R.color.holo_blue_light));
        } else {
            holder.itemView.setBackgroundColor(holder.itemView.getContext()
                    .getResources().getColor(android.R.color.white));
        }
    }

    @Override
    public int getItemCount() {
        return instructions != null ? instructions.size() : 0;
    }

    public void setCurrentStep(int position) {
        currentStep = position;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvInstruction, tvDistance;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInstruction = itemView.findViewById(R.id.tvStepInstruction);
            tvDistance = itemView.findViewById(R.id.tvStepDistance);
        }
    }
}