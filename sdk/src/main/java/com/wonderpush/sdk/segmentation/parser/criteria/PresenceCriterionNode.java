package com.wonderpush.sdk.segmentation.parser.criteria;

import com.wonderpush.sdk.segmentation.parser.ASTCriterionNode;
import com.wonderpush.sdk.segmentation.parser.ASTCriterionVisitor;
import com.wonderpush.sdk.segmentation.parser.ParsingContext;

public class PresenceCriterionNode extends ASTCriterionNode {

    public final boolean present;
    public final ASTCriterionNode sinceDateComparison;
    public final ASTCriterionNode elapsedTimeComparison;

    public PresenceCriterionNode(ParsingContext context, boolean present, ASTCriterionNode sinceDateComparison, ASTCriterionNode elapsedTimeComparison) {
        super(context);
        this.present = present;
        this.sinceDateComparison = sinceDateComparison;
        this.elapsedTimeComparison = elapsedTimeComparison;
    }

    @Override
    public <T> T accept(ASTCriterionVisitor<T> visitor) {
        return visitor.visitPresenceCriterionNode(this);
    }

}
