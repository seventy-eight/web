package org.seventyeight.ast;

import java.util.ArrayList;
import java.util.List;

/**
 * @author cwolfgang
 */
public class StatementBlock extends AbstractStatement {

    protected Statement parent;

    protected List<Statement> statements = new ArrayList<Statement>(  );

    @Override
    public void accept( Visitor visitor ) {
        visitor.visit( this );
    }

    public List<Statement> getStatements() {
        return statements;
    }

    public void addStatement(Statement statement) {
        statement.setParent( this );
        statements.add( statement );
    }

    public int size() {
        return statements.size();
    }
}
