using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace Moment.Api.Migrations
{
    /// <inheritdoc />
    public partial class AddUserCurrentVibe : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<string>(
                name: "CurrentVibe",
                table: "Users",
                type: "text",
                nullable: true);
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "CurrentVibe",
                table: "Users");
        }
    }
}
